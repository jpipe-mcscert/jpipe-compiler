package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.visitor.JpipeExporter;

/**
 * Compilation step that serialises a {@link Unit} to {@code .jd} source text.
 */
public class ExportToJpipe extends Transformation<Unit, String> {

	@Override
	protected String run(Unit input, CompilationContext ctx) throws Exception {
		return new JpipeExporter().export(input);
	}
}
