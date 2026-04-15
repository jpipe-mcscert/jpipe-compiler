package ca.mcscert.jpipe.model.elements;

/**
 * Read-only view of an element's identity: its {@code id} and {@code label}.
 *
 * <p>
 * Implemented by {@link JustificationElement} (live model elements) and
 * {@link ca.mcscert.jpipe.commands.creation.ElementCreationCommand}
 * (construction commands), allowing equivalence relations and partition
 * utilities to operate uniformly over both without requiring one to wrap the
 * other.
 */
public interface ElementView {

	/** Unique identifier of this element within its containing model. */
	String id();

	/** Human-readable display label. */
	String label();
}
