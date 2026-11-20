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
import java.util.Optional;
import ca.mcscert.jpipe.commands.PendingCommands;
import ca.mcscert.jpipe.model.JustificationModel;
import java.util.List;

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

	@Override
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
		JustificationElement supportable = resolve(model, supportableId);
		JustificationElement supporter = resolve(model, supporterId);

		// Check the resolved element ids (findById resolves plain ids to their
		// qualified form), otherwise a plain id would bypass the restriction.
		if (model.isInheritedNonSupport(supportable.id())) {
			throw new ReferenceIntoTemplateException(supportable.id(),
					container);
		}
		if (model.isInheritedNonSupport(supporter.id())) {
			throw new ReferenceIntoTemplateException(supporter.id(), container);
		}

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

	/**
	 * Waits, beyond both ids resolving, until every element of the model is
	 * declared: no element creation, {@code implements} or override still
	 * pending in it. A short id then resolves to the element it names, not to
	 * an inherited one that happens to exist first, and a link into an abstract
	 * support follows its override wherever it is written (#177). Other links
	 * do not hold it back.
	 */
	@Override
	public boolean ready(Unit unit, PendingCommands pending) {
		return condition().test(unit)
				&& !pending.target(container, AddSupport.class::isInstance);
	}

	/**
	 * The element {@code id} names in {@code model}: its own element of that
	 * id, or the one inherited element whose id ends with it. Several inherited
	 * ones are an ambiguity, reported rather than settled by order (#177).
	 */
	private JustificationElement resolve(JustificationModel model, String id) {
		List<JustificationElement> ambiguous = model.ambiguousMatches(id);
		if (!ambiguous.isEmpty()) {
			throw new AmbiguousIdException(id, container, ambiguous);
		}
		return model.findById(id).orElseThrow(
				() -> new NoSuchElementException("No element with id: " + id));
	}

	@Override
	public Optional<String> target() {
		return Optional.of(container);
	}

	@Override
	public String toString() {
		return "support('" + container + "', '" + supportableId + "', '"
				+ supporterId + "').";
	}
}
