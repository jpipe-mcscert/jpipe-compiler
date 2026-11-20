package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class OutputTargetTest {

	@TempDir
	Path dir;

	/** Standing for standard output: the file tests never write to it. */
	private final OutputStream console = new ByteArrayOutputStream();

	@Test
	void the_file_is_written_only_by_the_commit() throws IOException {
		Path file = Files.writeString(dir.resolve("out.json"), "previous");
		OutputTarget target = OutputTarget.open(file.toString(), console);

		target.stream().write(bytes("new"));
		assertThat(file).hasContent("previous");
		target.commit();

		assertThat(file).hasContent("new");
		assertThat(entries()).containsExactly(file);
	}

	@Test
	void an_output_never_committed_leaves_the_previous_file()
			throws IOException {
		// A failed run used to leave the file truncated to 0 bytes (#174).
		Path file = Files.writeString(dir.resolve("out.dot"), "precious");
		OutputTarget target = OutputTarget.open(file.toString(), console);

		target.stream().write(bytes("half an export"));

		assertThat(file).hasContent("precious");
		assertThat(entries()).containsExactly(file);
	}

	@Test
	void a_new_file_is_created_only_by_the_commit() throws IOException {
		Path file = dir.resolve("new.json");
		OutputTarget target = OutputTarget.open(file.toString(), console);

		target.stream().write(bytes("{}"));
		assertThat(file).doesNotExist();
		target.commit();

		assertThat(file).hasContent("{}");
	}

	@Test
	void writing_through_a_symbolic_link_keeps_the_link() throws IOException {
		Path real = Files.writeString(dir.resolve("real.json"), "previous");
		Path link = Files.createSymbolicLink(dir.resolve("link.json"), real);
		OutputTarget target = OutputTarget.open(link.toString(), console);

		target.stream().write(bytes("new"));
		target.commit();

		assertThat(link).isSymbolicLink();
		assertThat(real).hasContent("new");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void writing_over_a_file_keeps_what_it_is() throws IOException {
		// Written in place: permissions, owner and hard links survive.
		Path file = Files.writeString(dir.resolve("out.py"), "previous");
		Files.setPosixFilePermissions(file,
				PosixFilePermissions.fromString("rwxr-x---"));
		Path hardLink = Files.createLink(dir.resolve("shared.py"), file);
		OutputTarget target = OutputTarget.open(file.toString(), console);

		target.stream().write(bytes("new"));
		target.commit();

		assertThat(PosixFilePermissions
				.toString(Files.getPosixFilePermissions(file)))
				.isEqualTo("rwxr-x---");
		assertThat(hardLink).hasContent("new");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void a_device_is_written_to_rather_than_replaced() throws IOException {
		// `-o /dev/null` must work, as it did before #174.
		OutputTarget target = OutputTarget.open("/dev/null", console);

		target.stream().write(bytes("discarded"));

		assertThatCode(target::commit).doesNotThrowAnyException();
		assertThat(Files.isRegularFile(Path.of("/dev/null"))).isFalse();
	}

	@Test
	void a_directory_is_refused_up_front() {
		String path = dir.toString();

		assertThatThrownBy(() -> OutputTarget.open(path, console))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessage("cannot write '" + path + "': is a directory");
	}

	@Test
	void a_missing_directory_is_refused_up_front() {
		String path = dir.resolve("nowhere/out.json").toString();

		assertThatThrownBy(() -> OutputTarget.open(path, console))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessageStartingWith("cannot write '" + path + "': ")
				.hasMessageContaining("no such directory");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void a_read_only_file_is_refused_up_front() throws IOException {
		Path file = Files.writeString(dir.resolve("locked.json"), "previous");
		Files.setPosixFilePermissions(file,
				PosixFilePermissions.fromString("r--r--r--"));
		String path = file.toString();

		assertThatThrownBy(() -> OutputTarget.open(path, console))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessage("cannot write '" + path + "': permission denied");
		assertThat(file).hasContent("previous");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void a_new_file_in_a_read_only_directory_is_refused_up_front()
			throws IOException {
		Path locked = Files.createDirectory(dir.resolve("locked"));
		Files.setPosixFilePermissions(locked,
				PosixFilePermissions.fromString("r-xr-xr-x"));
		String path = locked.resolve("new.json").toString();
		try {
			assertThatThrownBy(() -> OutputTarget.open(path, console))
					.isInstanceOf(UnwritableOutputException.class).hasMessage(
							"cannot write '" + path + "': permission denied");
		} finally {
			// Let @TempDir clean up after the test.
			Files.setPosixFilePermissions(locked,
					PosixFilePermissions.fromString("rwxr-xr-x"));
		}
	}

	@Test
	void a_failing_standard_output_is_reported_rather_than_swallowed()
			throws IOException {
		try (PrintStream broken = new PrintStream(new FailingStream())) {
			OutputTarget target = OutputTarget.open(CompilationConfig.STDOUT,
					broken);
			target.stream().write(bytes("lost"));

			assertThatThrownBy(target::commit)
					.isInstanceOf(UnwritableOutputException.class)
					.hasMessage("cannot write to standard output: write error");
		}
	}

	@Test
	void the_same_existing_file_is_detected() throws IOException {
		Path file = Files.writeString(dir.resolve("same.jd"), "source");
		Path alias = Files.createSymbolicLink(dir.resolve("alias.jd"), file);

		assertThat(OutputTarget.sameFile(file.toString(), alias.toString()))
				.isTrue();
		assertThat(OutputTarget.sameFile(file.toString(),
				dir.resolve("other.jd").toString())).isFalse();
		assertThat(OutputTarget.sameFile(CompilationConfig.STDIN,
				CompilationConfig.STDOUT)).isFalse();
	}

	/** What the directory holds: nothing but the target, ever. */
	private List<Path> entries() throws IOException {
		try (Stream<Path> entries = Files.list(dir)) {
			return entries.toList();
		}
	}

	private static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	/** A stream whose every write fails, as a full disk or closed pipe does. */
	private static final class FailingStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			throw new IOException("No space left on device");
		}
	}
}
