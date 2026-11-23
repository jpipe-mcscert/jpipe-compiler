package ca.mcscert.jpipe.model.validation;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Origin;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.Violation;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
 * <li>{@code abstract-support-used} — every {@link AbstractSupport} supports a
 * strategy, since one that supports none can never be overridden.
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
	private static final String ABSTRACT_SUPPORT_USED = "abstract-support-used";

	/**
	 * Validates all models in the unit. Violations carry source locations
	 * resolved from the unit's location registry.
	 */
	public List<Violation> validate(Unit unit) {
		ValidationContext ctx = ValidationContext.of(unit);
		List<Violation> violations = new ArrayList<>();
		// A model that could not be built in full has its cause reported
		// already; its gaps are consequences, not mistakes of their own.
		// An unused @support is one mistake, wherever it is copied: a template
		// built by an operator call keeps its sources' placeholders, at the
		// place they are declared (#183). A call runs once its sources are
		// built, so a source comes before its result in the unit, and the
		// template that declares the @support is the one that reports it.
		Set<SourceLocation> unusedSupports = new HashSet<>();
		for (JustificationModel model : unit.getModels()) {
			if (!unit.isComplete(model.getName())) {
				continue;
			}
			for (Violation v : checkModel(model, ctx)) {
				if (!isCopyOfAnUnusedSupport(v, unusedSupports)) {
					violations.add(v);
				}
			}
		}
		return violations;
	}

	/**
	 * Whether {@code v} reports a @support already reported at the same place:
	 * a copy of it. Records {@code v}'s place otherwise.
	 */
	private static boolean isCopyOfAnUnusedSupport(Violation v,
			Set<SourceLocation> reported) {
		return v.rule().equals(ABSTRACT_SUPPORT_USED) && v.location().isKnown()
				&& !reported.add(v.location());
	}

	/**
	 * Validates a single model without location data. All violations carry
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public List<Violation> validateModel(JustificationModel model) {
		return checkModel(model, ValidationContext.STANDALONE);
	}

	// -------------------------------------------------------------------------

	private List<Violation> checkModel(JustificationModel model,
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
		// One whose override failed is reported as that failure.
		justification.elementsOfType(AbstractSupport.class).stream()
				.filter(as -> !overrideFailed(justification, as.id(), ctx))
				.forEach(as -> violations.add(new Violation(
						"no-abstract-support",
						"Abstract support '" + as.id() + "' in justification '"
								+ name + "' was not overridden",
						ctx.locationOf(name, as.id()))));
	}

	/**
	 * Whether an override of {@code id} failed in {@code model}, or in a
	 * template it inherits {@code id} from, under the id the copy has there:
	 * the placeholder is followed through the origins of its copies (#209).
	 */
	private static boolean overrideFailed(JustificationModel model, String id,
			ValidationContext ctx) {
		JustificationModel current = model;
		String local = id;
		while (true) {
			if (ctx.overrideFailed(current.getName(), local)) {
				return true;
			}
			Optional<Origin> origin = current.inheritedOrigin(local);
			if (origin.isEmpty()) {
				return false;
			}
			current = origin.get().model();
			local = origin.get().id();
		}
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
		// abstract-support-used: an abstract support that supports no
		// strategy can never be overridden, so it is reported in the template
		// that declares it, rather than in every model implementing that
		// template (#183).
		Set<String> inherited = template.inheritedElements().stream()
				.map(JustificationElement::id).collect(Collectors.toSet());
		template.abstractSupports().stream()
				.filter(as -> !inherited.contains(as.id()))
				.filter(as -> template.strategiesSupportedBy(as.id()).isEmpty())
				.forEach(as -> violations.add(new Violation(
						ABSTRACT_SUPPORT_USED,
						"Abstract support '" + as.id() + "' in template '"
								+ name + "' supports no strategy",
						ctx.locationOf(name, as.id()))));
	}
}
