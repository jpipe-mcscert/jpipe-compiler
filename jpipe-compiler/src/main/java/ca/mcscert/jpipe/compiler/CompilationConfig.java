package ca.mcscert.jpipe.compiler;

/**
 * Immutable configuration for a single process-mode compilation run.
 *
 * <p>
 * Use {@link #STDIN} / {@link #STDOUT} as sentinel values for {@code inputFile}
 * / {@code outputFile} when reading from standard input or writing to standard
 * output.
 *
 * @param inputFile
 *            path to the {@code .jd} source file, or {@link #STDIN}.
 * @param outputFile
 *            path to the output file, or {@link #STDOUT}.
 * @param format
 *            output format.
 * @param diagramName
 *            name of the model to export.
 */
public record CompilationConfig(String inputFile, String outputFile,
		Format format, String diagramName) {

	public static final String STDIN = "<stdin>";
	public static final String STDOUT = "<stdout>";

	/**
	 * Whether {@code source} is standard input. It names no file, and is not
	 * even a valid path on Windows: never turn it into a {@code Path}.
	 */
	public static boolean isStdin(String source) {
		return STDIN.equals(source);
	}

	/** Whether {@code target} is standard output. */
	public static boolean isStdout(String target) {
		return STDOUT.equals(target);
	}
}
