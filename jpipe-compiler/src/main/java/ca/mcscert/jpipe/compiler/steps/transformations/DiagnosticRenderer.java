package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot;
import ca.mcscert.jpipe.compiler.model.Transformation;

/**
 * Common base of the {@code diagnostic} command's renderers.
 *
 * <p>
 * Rendering is exposed as {@link #render(DiagnosticSnapshot)}, callable
 * directly and not only as a pipeline step. That is what lets a compilation
 * which aborted still be reported on: a fatal diagnostic stops the pipeline at
 * the next {@link Transformation#fire} boundary (ADR-0016), so a report that
 * could only be produced as a step could never describe the very failures it
 * exists to report.
 *
 * @see DiagnosticReport
 * @see JsonDiagnosticReport
 */
public abstract class DiagnosticRenderer
		extends
			Transformation<DiagnosticSnapshot, String> {

	/**
	 * Renders a snapshot into the report text.
	 *
	 * @param snapshot
	 *            what the compilation produced and reported.
	 * @return the rendered report.
	 */
	public abstract String render(DiagnosticSnapshot snapshot);

	@Override
	protected final String run(DiagnosticSnapshot input,
			CompilationContext ctx) {
		return render(input);
	}
}
