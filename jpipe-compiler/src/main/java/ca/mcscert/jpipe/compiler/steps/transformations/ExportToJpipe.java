package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.visitor.JpipeExporter;

/**
 * Compilation step that serialises a {@link JustificationModel} to {@code .jd}
 * source text.
 */
public class ExportToJpipe
		extends
			Transformation<JustificationModel<?>, String> {

	@Override
	protected String run(JustificationModel<?> input, CompilationContext ctx)
			throws Exception {
		return new JpipeExporter().export(input);
	}
}
