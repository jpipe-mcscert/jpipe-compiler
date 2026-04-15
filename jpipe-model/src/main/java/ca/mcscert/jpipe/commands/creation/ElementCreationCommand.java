package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.ElementView;

/**
 * Sealed interface common to all element-creation commands.
 *
 * <p>
 * Provides uniform access to the three fields shared by every element-creation
 * command ({@code container}, {@code identifier}, {@code label}) and a
 * {@link #withId} factory that copies the command with a different identifier.
 * This eliminates repetitive {@code instanceof} chains in code that processes
 * creation commands generically (e.g.
 * {@link ca.mcscert.jpipe.operators.Unifier}).
 *
 * <p>
 * Also implements {@link ElementView} so that equivalence relations and
 * partition utilities can operate on commands directly, without wrapping them
 * in live model objects.
 */
public sealed interface ElementCreationCommand extends Command, ElementView
		permits CreateConclusion, CreateStrategy, CreateEvidence,
		CreateSubConclusion, CreateAbstractSupport {

	/** Name of the model this command adds an element to. */
	String container();

	/** Id assigned to the new element. */
	String identifier();

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Delegates to {@link #identifier()} so that {@link ElementView} callers
	 * see the same value as domain code that uses {@code identifier()}.
	 */
	@Override
	default String id() {
		return identifier();
	}

	/** Display label of the new element. */
	String label();

	/** Source location of the element declaration. */
	SourceLocation location();

	/**
	 * Returns a copy of this command with {@code newId} as the element
	 * identifier. The container, label, and location are preserved.
	 */
	ElementCreationCommand withId(String newId);
}
