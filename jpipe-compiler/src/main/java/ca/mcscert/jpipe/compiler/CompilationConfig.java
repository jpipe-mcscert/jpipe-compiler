package ca.mcscert.jpipe.compiler;

/**
 * Immutable configuration for a single compilation run.
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
 * @param mode
 *            whether to run in {@link Mode#DIAGNOSTIC} or {@link Mode#PROCESS}
 *            mode.
 * @param format
 *            output format, relevant only in {@link Mode#PROCESS} mode.
 * @param diagramName
 *            name of the model to export; {@code null} means auto-select when
 *            unambiguous.
 */
public record CompilationConfig(String inputFile, String outputFile, Mode mode,
		Format format, String diagramName) {

	public static final String STDIN = "<stdin>";
	public static final String STDOUT = "<stdout>";
}
