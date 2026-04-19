package ca.mcscert.jpipe.model.validation;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.Violation;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that justification models are structurally complete.
 *
 * <p>
 * Rules applied to all models:
 * <ul>
 * <li>{@code conclusion-present} — model has exactly one
 * {@link ca.mcscert.jpipe.model.elements.Conclusion}.
 * <li>{@code conclusion-supported} — the conclusion has a supporting
 * {@link ca.mcscert.jpipe.model.elements.Strategy}.
 * <li>{@code strategy-supported} — every strategy has at least one supporting
 * leaf.
 * <li>{@code sub-conclusion-supported} — every sub-conclusion has a supporting
 * strategy.
 * </ul>
 *
 * <p>
 * Rules applied to {@link Justification} only:
 * <ul>
 * <li>{@code no-abstract-support} — no {@link AbstractSupport} placeholders
 * remain (they are only valid in templates).
 * </ul>
 *
 * <p>
 * Rules applied to {@link Template} only:
 * <ul>
 * <li>{@code has-abstract-support} — template declares at least one
 * {@link AbstractSupport}.
 * </ul>
 *
 * <p>
 * Use {@link #validate(Unit)} for location-aware validation within a compiler
 * pipeline. Use {@link #validateModel(JustificationModel)} for standalone use
 * when no {@link Unit} is available; violations will carry
 * {@link SourceLocation#UNKNOWN}.
 */
public final class CompletenessValidator {

	private static final String IN_MODEL = "' in model '";

	/**
	 * Validates all models in the unit. Violations carry source locations
	 * resolved from the unit's location registry.
	 */
	public List<Violation> validate(Unit unit) {
		ValidationContext ctx = ValidationContext.of(unit);
		List<Violation> violations = new ArrayList<>();
		unit.getModels()
				.forEach(model -> violations.addAll(checkModel(model, ctx)));
		return violations;
	}

	/**
	 * Validates a single model without location data. All violations carry
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public List<Violation> validateModel(JustificationModel<?> model) {
		return checkModel(model, ValidationContext.STANDALONE);
	}

	// -------------------------------------------------------------------------

	private List<Violation> checkModel(JustificationModel<?> model,
			ValidationContext ctx) {
		List<Violation> violations = new ArrayList<>();
		String name = model.getName();

		// conclusion-present
		var conclusionOpt = model.conclusion();
		if (conclusionOpt.isEmpty()) {
			violations.add(new Violation("conclusion-present",
					"Model '" + name + "' has no conclusion",
					ctx.locationOf(name)));
		} else {
			// conclusion-supported (only meaningful when conclusion exists)
			var conclusion = conclusionOpt.get();
			if (conclusion.getSupport().isEmpty()) {
				String cId = conclusion.id();
				violations.add(new Violation("conclusion-supported",
						"Conclusion '" + cId + IN_MODEL + name
								+ "' has no supporting strategy",
						ctx.locationOf(name, cId)));
			}
		}

		// strategy-supported
		model.strategies().stream().filter(s -> s.getSupports().isEmpty())
				.forEach(s -> violations.add(new Violation("strategy-supported",
						"Strategy '" + s.id() + IN_MODEL + name
								+ "' has no supporting element",
						ctx.locationOf(name, s.id()))));

		// sub-conclusion-supported
		model.subConclusions().stream().filter(sc -> sc.getSupport().isEmpty())
				.forEach(sc -> violations
						.add(new Violation("sub-conclusion-supported",
								"Sub-conclusion '" + sc.id() + IN_MODEL + name
										+ "' has no supporting strategy",
								ctx.locationOf(name, sc.id()))));

		// model-type-specific rules
		switch (model) {
			case Justification j ->
				checkJustification(j, name, ctx, violations);
			case Template t -> checkTemplate(t, name, ctx, violations);
		}

		return violations;
	}

	private void checkJustification(Justification justification, String name,
			ValidationContext ctx, List<Violation> violations) {
		// no-abstract-support: AbstractSupports must have been replaced by
		// override commands before the completeness check runs.
		// Note: elementsOfType() accesses the raw elements list from within
		// JustificationModel (where E is erased to JustificationElement),
		// avoiding ClassCastException when AbstractSupport is stored in a
		// Justification's List<CommonElement> via unchecked cast.
		justification.elementsOfType(AbstractSupport.class).forEach(
				as -> violations.add(new Violation("no-abstract-support",
						"Abstract support '" + as.id() + "' in justification '"
								+ name + "' was not overridden",
						ctx.locationOf(name, as.id()))));
	}

	private void checkTemplate(Template template, String name,
			ValidationContext ctx, List<Violation> violations) {
		// has-abstract-support: a template with no abstract supports is a
		// justification in disguise.
		if (template.abstractSupports().isEmpty()) {
			violations.add(new Violation("has-abstract-support",
					"Template '" + name + "' declares no abstract supports",
					ctx.locationOf(name)));
		}
	}
}
