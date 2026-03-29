package ca.mcscert.jpipe.compiler.steps.checkers;

import ca.mcscert.jpipe.compiler.model.Checker;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.validation.ConsistencyValidator;

/**
 * Pipeline step that checks all models in a {@link Unit} for structural
 * consistency. Delegates rule evaluation to {@link ConsistencyValidator} and
 * maps each violation to a non-fatal ERROR diagnostic, enriched with source
 * location from the unit's location registry.
 */
public final class ConsistencyChecker extends Checker<Unit> {

	@Override
	protected void check(Unit unit, CompilationContext ctx) {
		new ConsistencyValidator().validate(unit).forEach(v -> {
			String msg = "[" + v.rule() + "] " + v.message();
			if (v.location().isKnown()) {
				ctx.error(v.location().line(), v.location().column(), msg);
			} else {
				ctx.error(msg);
			}
		});
	}
}
