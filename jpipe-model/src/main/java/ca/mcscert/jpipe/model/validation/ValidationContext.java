package ca.mcscert.jpipe.model.validation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.Optional;

/**
 * Carries optional location data through validation rules.
 *
 * <p>
 * Replaces the nullable {@link Unit} parameter used by validator helper
 * methods: validators use {@link #of(Unit)} when called from
 * {@link ConsistencyValidator#validate(Unit)} or
 * {@link CompletenessValidator#validate(Unit)}, and {@link #STANDALONE} when
 * called from {@code validateModel()} without a surrounding compilation unit.
 */
record ValidationContext(Optional<Unit> unit) {

	/**
	 * Context with no location data; all lookups return
	 * {@link SourceLocation#UNKNOWN}.
	 */
	static final ValidationContext STANDALONE = new ValidationContext(
			Optional.empty());

	/** Creates a context backed by the given unit's location registry. */
	static ValidationContext of(Unit unit) {
		return new ValidationContext(Optional.of(unit));
	}

	/**
	 * Resolves the declared location of a model, or
	 * {@link SourceLocation#UNKNOWN}.
	 */
	SourceLocation locationOf(String modelName) {
		return unit.map(u -> u.locationOf(modelName))
				.orElse(SourceLocation.UNKNOWN);
	}

	/**
	 * Resolves the declared location of an element inside a model, or
	 * {@link SourceLocation#UNKNOWN}.
	 */
	SourceLocation locationOf(String modelName, String elementId) {
		return unit.map(u -> u.locationOf(modelName, elementId))
				.orElse(SourceLocation.UNKNOWN);
	}
}
