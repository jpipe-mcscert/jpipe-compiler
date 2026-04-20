package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.visitor.DotExporter;

/**
 * Compilation step that serialises a {@link JustificationModel} to Graphviz DOT
 * text.
 */
public class ExportToDot extends Transformation<JustificationModel<?>, String> {

	@Override
	protected String run(JustificationModel<?> input, CompilationContext ctx) {
		return new DotExporter().export(input);
	}
}
