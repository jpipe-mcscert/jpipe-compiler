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
}
