package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.visitor.AmbiguousIdentifierException;
import ca.mcscert.jpipe.visitor.PythonExporter;

/**
 * Compilation step that serialises a {@link JustificationModel} to Python
 * source text.
 *
 * <p>
 * A model in which one id designates two elements cannot be exported: the
 * module would bind both functions to the same id. The consistency checker has
 * already reported the cause as an error, so the refusal ends the compilation
 * like any other failed step, rather than as an unexpected exception (#182).
 */
public class ExportToPython extends Transformation<JustificationModel, String> {

	@Override
	protected String run(JustificationModel input, CompilationContext ctx) {
		try {
			return new PythonExporter().export(input, ctx.sourcePath());
		} catch (AmbiguousIdentifierException e) {
			throw new CompilationException(stepName(), e.getMessage());
		}
	}
}
