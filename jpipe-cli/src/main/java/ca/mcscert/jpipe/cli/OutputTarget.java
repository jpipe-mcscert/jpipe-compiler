package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Where a command writes its output: standard output, or a file given with
 * {@code -o}.
 *
 * <p>
 * A file is never written in place. The output goes to a hidden temporary file
 * in the same directory, which {@link #commit()} moves over the target once the
 * command has succeeded; {@link #close()} without a commit deletes it. A run
 * that fails therefore leaves the previous file as it was, rather than
 * truncated before compilation even started (#174).
 *
 * <p>
 * Every failure to write, whether it happens when the target is opened, during
 * the export or when the file is moved into place, is raised as an
 * {@link UnwritableOutputException} naming the target. That includes standard
 * output, whose {@link PrintStream} never throws and only records the error.
 */
final class OutputTarget implements Closeable {

	private static final Logger logger = LogManager.getLogger();

	private final String name;
	private final Path target;
	private final Path temporary;
	private final OutputStream raw;
	private final Guard guarded;
	private boolean committed;

	private OutputTarget(String name, Path target, Path temporary,
			OutputStream raw) {
		this.name = name;
		this.target = target;
		this.temporary = temporary;
		this.raw = raw;
		this.guarded = new Guard(raw);
	}

	/**
	 * Opens the target named by {@code -o}: standard output for
	 * {@link CompilationConfig#STDOUT}, a file otherwise.
	 *
	 * @throws UnwritableOutputException
	 *             if the file cannot be written: an invalid path, a directory,
	 *             a missing parent directory, or no permission.
	 */
	static OutputTarget open(String output) throws UnwritableOutputException {
		if (CompilationConfig.STDOUT.equals(output)) {
			return new OutputTarget(output, null, null, System.out);
		}
		Path target = resolve(output);
		Path temporary = target.resolveSibling(
				"." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");
		try {
			OutputStream raw = Files.newOutputStream(temporary,
					StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			keepPermissions(target, temporary);
			return new OutputTarget(output, target, temporary, raw);
		} catch (IOException e) {
			throw new UnwritableOutputException(output, reason(e), e);
		}
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

	/** The stream a command writes to. */
	OutputStream stream() {
		return guarded;
	}

	/**
	 * Makes the output final: flushes it, and moves a file into place over the
	 * previous one. Called only once the command has succeeded.
	 *
	 * @throws UnwritableOutputException
	 *             if the output could not be written or moved into place.
	 */
	void commit() throws UnwritableOutputException {
		guarded.flush();
		if (temporary == null) {
			return;
		}
		try {
			raw.close();
			move(temporary, target);
			committed = true;
		} catch (IOException e) {
			throw new UnwritableOutputException(name, reason(e), e);
		}
	}

	/**
	 * Discards an output that was not committed, leaving the previous file
	 * untouched. Standard output is never closed: the command borrows it.
	 */
	@Override
	public void close() {
		if (temporary == null || committed) {
			return;
		}
		try {
			raw.close();
			Files.deleteIfExists(temporary);
		} catch (IOException e) {
			logger.warn("Could not delete the temporary file {}: {}", temporary,
					e.getMessage());
		}
	}

	/**
	 * The path to write to: the file a symbolic link points to, so that the
	 * link survives, in a directory that exists.
	 */
	private static Path resolve(String output)
			throws UnwritableOutputException {
		Path target;
		try {
			target = Path.of(output).toAbsolutePath();
		} catch (InvalidPathException e) {
			throw new UnwritableOutputException(output, "invalid path", e);
		}
		try {
			if (Files.exists(target)) {
				target = target.toRealPath();
			}
		} catch (IOException e) {
			throw new UnwritableOutputException(output, reason(e), e);
		}
		if (Files.isDirectory(target)) {
			throw new UnwritableOutputException(output, "is a directory");
		}
		if (Files.exists(target) && !Files.isWritable(target)) {
			throw new UnwritableOutputException(output, "permission denied");
		}
		if (!Files.isDirectory(target.getParent())) {
			throw new UnwritableOutputException(output,
					"no such directory '" + target.getParent() + "'");
		}
		return target;
	}

	/**
	 * Gives the temporary file the permissions of the file it replaces, so that
	 * writing over a file does not change who may read it. Best effort: a file
	 * system without POSIX permissions keeps its defaults.
	 */
	private static void keepPermissions(Path target, Path temporary) {
		if (!Files.exists(target)) {
			return;
		}
		try {
			Files.setPosixFilePermissions(temporary,
					Files.getPosixFilePermissions(target));
		} catch (UnsupportedOperationException | IOException _) {
			// No POSIX permissions here: the defaults are all there is.
		}
	}

	private static void move(Path from, Path to) throws IOException {
		try {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException _) {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
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
	 * Turns every failure to write into an {@link UnwritableOutputException}
	 * naming the target, and catches the errors a {@link PrintStream} records
	 * instead of throwing.
	 */
	private final class Guard extends FilterOutputStream {

		Guard(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			try {
				out.write(b);
			} catch (IOException e) {
				throw new UnwritableOutputException(name, reason(e), e);
			}
			check();
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			try {
				out.write(b, off, len);
			} catch (IOException e) {
				throw new UnwritableOutputException(name, reason(e), e);
			}
			check();
		}

		@Override
		public void flush() throws UnwritableOutputException {
			try {
				out.flush();
			} catch (IOException e) {
				throw new UnwritableOutputException(name, reason(e), e);
			}
			check();
		}

		/** Closing is {@link OutputTarget}'s job, not the command's. */
		@Override
		public void close() {
			// intentionally empty: see OutputTarget#commit and #close
		}

		private void check() throws UnwritableOutputException {
			if (out instanceof PrintStream printStream
					&& printStream.checkError()) {
				throw new UnwritableOutputException(name, "write error");
			}
		}
	}
}
