package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.steps.io.sinks.WriteErrors;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Where a command writes its output: standard output, or a file given with
 * {@code -o}.
 *
 * <p>
 * A file is not opened while the command runs. Its output is held in memory,
 * and {@link #commit()} writes it to the file, in place, once the command has
 * produced it. A run that aborts never reaches the commit, so it leaves the
 * previous file as it was, rather than truncated before compilation even
 * started (#174). Writing in place, rather than renaming a temporary file over
 * the target, keeps what the target is: a device such as {@code /dev/null}, a
 * pipe, a hard link, a file owned by someone else in a shared directory.
 *
 * <p>
 * Every failure to write is raised as an {@link UnwritableOutputException}
 * naming the target. That includes standard output, whose
 * {@link java.io.PrintStream} never throws and only records the error.
 */
final class OutputTarget {

	private final String name;
	/** Standard output, or {@code null} for a file. */
	private final Borrowed stdout;
	/** The file to write, or {@code null} for standard output. */
	private final Path file;
	/** What the command wrote, for a file. */
	private final ByteArrayOutputStream buffer;

	private OutputTarget(String name, Borrowed stdout, Path file) {
		this.name = name;
		this.stdout = stdout;
		this.file = file;
		this.buffer = file == null ? null : new ByteArrayOutputStream();
	}

	/**
	 * Opens the target named by {@code -o}: {@code stdout} for
	 * {@link CompilationConfig#STDOUT}, a file otherwise. A file is checked
	 * now, so that a target that cannot be written fails before compiling, but
	 * it is written only by {@link #commit()}.
	 *
	 * @param stdout
	 *            the standard output, given by the command: it decides where
	 *            the console is, this class only writes to it.
	 * @throws UnwritableOutputException
	 *             if the file cannot be written: an invalid path, a directory,
	 *             a missing parent directory, or no permission.
	 */
	static OutputTarget open(String output, OutputStream stdout)
			throws UnwritableOutputException {
		if (CompilationConfig.STDOUT.equals(output)) {
			return new OutputTarget(output, new Borrowed(stdout), null);
		}
		return new OutputTarget(output, null, check(output));
	}

	/**
	 * Whether {@code input} and {@code output} name the same existing file.
	 * Compiling would then replace the source with the export, or with the
	 * report, so the commands refuse to run (#174).
	 */
	static boolean sameFile(String input, String output) {
		if (CompilationConfig.STDIN.equals(input)
				|| CompilationConfig.STDOUT.equals(output)) {
			return false;
		}
		try {
			return Files.isSameFile(Path.of(input), Path.of(output));
		} catch (IOException | InvalidPathException _) {
			// One of them does not exist or cannot be named: not the same
			// file, and whatever is wrong is reported when it is used.
			return false;
		}
	}

	/** The stream a command writes to. Not to be closed. */
	OutputStream stream() {
		return stdout != null ? stdout : buffer;
	}

	/**
	 * Makes the output final: flushes standard output, or writes the file.
	 * Called once the command has produced its output; never after an abort.
	 *
	 * @throws UnwritableOutputException
	 *             if the output could not be written.
	 */
	void commit() throws UnwritableOutputException {
		try {
			if (stdout != null) {
				WriteErrors.flushAndCheck(stdout.out());
			} else {
				Files.write(file, buffer.toByteArray());
			}
		} catch (IOException e) {
			throw new UnwritableOutputException(name, reason(e), e);
		}
	}

	/** Refuses a file that cannot be written, before anything runs. */
	private static Path check(String output) throws UnwritableOutputException {
		Path file;
		try {
			file = Path.of(output).toAbsolutePath();
		} catch (InvalidPathException e) {
			throw new UnwritableOutputException(output, "invalid path", e);
		}
		if (Files.isDirectory(file)) {
			throw new UnwritableOutputException(output, "is a directory");
		}
		if (Files.exists(file) && !Files.isWritable(file)) {
			throw new UnwritableOutputException(output, "permission denied");
		}
		if (!Files.exists(file) && !Files.isDirectory(file.getParent())) {
			throw new UnwritableOutputException(output,
					"no such directory '" + file.getParent() + "'");
		}
		return file;
	}

	/** Why a file operation failed, in words rather than an exception name. */
	static String reason(IOException e) {
		return switch (e) {
			case AccessDeniedException _ -> "permission denied";
			case NoSuchFileException _ -> "no such file or directory";
			case FileSystemException fse when fse.getReason() != null ->
				fse.getReason();
			default -> e.getMessage() != null
					? e.getMessage()
					: e.getClass().getSimpleName();
		};
	}

	/**
	 * Standard output, lent to the command: writes go straight through, and
	 * closing it is not the command's call. Its errors are checked on
	 * {@link #commit()}.
	 */
	private static final class Borrowed extends FilterOutputStream {

		Borrowed(OutputStream out) {
			super(out);
		}

		OutputStream out() {
			return out;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
		}

		@Override
		public void close() {
			// intentionally empty: standard output outlives the command
		}
	}
}
