package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.visitor.JsonExporter;

/**
 * Compilation step that serialises a {@link JustificationModel} to JSON text.
 */
public class ExportToJson
		extends
			Transformation<JustificationModel<?>, String> {

	@Override
	protected String run(JustificationModel<?> input, CompilationContext ctx)
			throws Exception {
		return new JsonExporter().export(input);
	}
}
