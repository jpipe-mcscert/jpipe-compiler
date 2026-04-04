package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.model.elements.SupportLeaf;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Establishes a support edge between two elements that already exist in the
 * same justification model.
 *
 * <p>
 * Valid pairs:
 * <ul>
 * <li>{@link Conclusion} supported by {@link Strategy}</li>
 * <li>{@link SubConclusion} supported by {@link Strategy}</li>
 * <li>{@link Strategy} supported by any {@link SupportLeaf}
 * ({@link ca.mcscert.jpipe.model.elements.Evidence}, {@link SubConclusion},
 * {@link ca.mcscert.jpipe.model.elements.AbstractSupport})</li>
 * </ul>
 *
 * <p>
 * The command defers execution until both elements are present in the model,
 * which handles the case where elements are declared out of order in the
 * source.
 */
public final class AddSupport extends RegularCommand {

	private final String container;
	private final String supportableId;
	private final String supporterId;
	private final SourceLocation location;

	public AddSupport(String container, String supportableId,
			String supporterId) {
		this(container, supportableId, supporterId, SourceLocation.UNKNOWN);
	}

	public AddSupport(String container, String supportableId,
			String supporterId, SourceLocation location) {
		this.container = container;
		this.supportableId = supportableId;
		this.supporterId = supporterId;
		this.location = location;
	}

	public String container() {
		return container;
	}

	public String supportableId() {
		return supportableId;
	}

	public String supporterId() {
		return supporterId;
	}

	public SourceLocation location() {
		return location;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container)
				.map(m -> m.findById(supportableId).isPresent()
						&& m.findById(supporterId).isPresent())
				.orElse(false);
	}

	@Override
	public void doExecute(Unit context) {
		var model = context.get(container);
		JustificationElement supportable = model.findById(supportableId)
				.map(e -> (JustificationElement) e)
				.orElseThrow(() -> new NoSuchElementException(
						"No element with id: " + supportableId));
		JustificationElement supporter = model.findById(supporterId)
				.map(e -> (JustificationElement) e)
				.orElseThrow(() -> new NoSuchElementException(
						"No element with id: " + supporterId));

		switch (supportable) {
			case Conclusion c when supporter instanceof Strategy s ->
				c.addSupport(s);
			case SubConclusion sc when supporter instanceof Strategy s ->
				sc.addSupport(s);
			case Strategy st when supporter instanceof SupportLeaf sl ->
				st.addSupport(sl);
			default -> throw new IllegalArgumentException("'" + supporterId
					+ "' cannot support '" + supportableId + "'");
		}
	}

	@Override
	public String toString() {
		return "support('" + container + "', '" + supportableId + "', '"
				+ supporterId + "').";
	}
}
