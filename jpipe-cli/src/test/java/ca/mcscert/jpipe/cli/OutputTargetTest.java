package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.CompilationConfig;
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

	@Test
	void a_committed_output_replaces_the_file() throws IOException {
		Path file = Files.writeString(dir.resolve("out.json"), "previous");

		try (OutputTarget target = OutputTarget.open(file.toString())) {
			target.stream().write(bytes("new"));
			assertThat(file).hasContent("previous");
			target.commit();
		}

		assertThat(file).hasContent("new");
		assertThat(entries()).containsExactly(file);
	}

	@Test
	void an_output_closed_without_a_commit_leaves_the_previous_file()
			throws IOException {
		// A failed run used to leave the file truncated to 0 bytes (#174).
		Path file = Files.writeString(dir.resolve("out.dot"), "precious");

		try (OutputTarget target = OutputTarget.open(file.toString())) {
			target.stream().write(bytes("half an export"));
		}

		assertThat(file).hasContent("precious");
		assertThat(entries()).containsExactly(file);
	}

	@Test
	void a_new_file_is_created_only_by_the_commit() throws IOException {
		Path file = dir.resolve("new.json");

		try (OutputTarget target = OutputTarget.open(file.toString())) {
			target.stream().write(bytes("{}"));
		}

		assertThat(file).doesNotExist();
	}

	@Test
	void writing_through_a_symbolic_link_keeps_the_link() throws IOException {
		Path real = Files.writeString(dir.resolve("real.json"), "previous");
		Path link = Files.createSymbolicLink(dir.resolve("link.json"), real);

		try (OutputTarget target = OutputTarget.open(link.toString())) {
			target.stream().write(bytes("new"));
			target.commit();
		}

		assertThat(link).isSymbolicLink();
		assertThat(real).hasContent("new");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void replacing_a_file_keeps_its_permissions() throws IOException {
		Path file = Files.writeString(dir.resolve("out.py"), "previous");
		Files.setPosixFilePermissions(file,
				PosixFilePermissions.fromString("rwxr-x---"));

		try (OutputTarget target = OutputTarget.open(file.toString())) {
			target.stream().write(bytes("new"));
			target.commit();
		}

		assertThat(PosixFilePermissions
				.toString(Files.getPosixFilePermissions(file)))
				.isEqualTo("rwxr-x---");
	}

	@Test
	void a_directory_is_refused_up_front() {
		String path = dir.toString();

		assertThatThrownBy(() -> OutputTarget.open(path))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessage("cannot write '" + path + "': is a directory");
	}

	@Test
	void a_missing_directory_is_refused_up_front() {
		String path = dir.resolve("nowhere/out.json").toString();

		assertThatThrownBy(() -> OutputTarget.open(path))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessageStartingWith("cannot write '" + path + "': ")
				.hasMessageContaining("no such directory");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void a_read_only_file_is_refused_rather_than_replaced() throws IOException {
		Path file = Files.writeString(dir.resolve("locked.json"), "previous");
		Files.setPosixFilePermissions(file,
				PosixFilePermissions.fromString("r--r--r--"));
		String path = file.toString();

		assertThatThrownBy(() -> OutputTarget.open(path))
				.isInstanceOf(UnwritableOutputException.class)
				.hasMessage("cannot write '" + path + "': permission denied");
		assertThat(file).hasContent("previous");
	}

	@Test
	void a_failing_standard_output_is_reported_rather_than_swallowed()
			throws IOException {
		PrintStream original = System.out;
		try (PrintStream broken = new PrintStream(new FailingStream())) {
			System.setOut(broken);
			try (OutputTarget target = OutputTarget
					.open(CompilationConfig.STDOUT)) {
				OutputStream out = target.stream();
				assertThatThrownBy(() -> out.write(bytes("lost")))
						.isInstanceOf(UnwritableOutputException.class)
						.hasMessage(
								"cannot write to standard output: write error");
			}
		} finally {
			System.setOut(original);
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

	/** What the directory holds: no temporary file may be left behind. */
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
