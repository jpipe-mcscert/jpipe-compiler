package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Transformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Extracts a single {@link JustificationModel} from a {@link Unit} by name, or
 * auto-selects it when the unit contains exactly one model and no name is
 * given.
 *
 * <p>
 * Use {@code -m} / {@code --model} on the CLI to supply the name.
 */
public class SelectModel extends Transformation<Unit, JustificationModel<?>> {

	private static final Logger logger = LogManager.getLogger();

	private final String diagramName;

	/**
	 * @param diagramName
	 *            name of the model to select, or {@code null} to auto-select
	 *            when unambiguous.
	 */
	public SelectModel(String diagramName) {
		this.diagramName = diagramName;
	}

	@Override
	protected JustificationModel<?> run(Unit unit, CompilationContext ctx)
			throws Exception {
		if (diagramName != null) {
			JustificationModel<?> model = unit.findModel(diagramName)
					.orElseThrow(() -> new CompilationException("SelectModel",
							"no model named '" + diagramName + "' in "
									+ ctx.sourcePath()));
			logger.debug("Selected model [{}]", diagramName);
			return model;
		}

		Collection<JustificationModel<?>> models = unit.getModels();
		if (models.size() == 1) {
			JustificationModel<?> model = models.iterator().next();
			logger.debug("Auto-selected model [{}]", model.getName());
			return model;
		}
		String available = models.stream().map(JustificationModel::getName)
				.collect(Collectors.joining(", "));
		logger.debug("Ambiguous selection — available: [{}]", available);
		throw new CompilationException("SelectModel",
				"source defines multiple models — use -d to specify one: "
						+ available);
	}
}
