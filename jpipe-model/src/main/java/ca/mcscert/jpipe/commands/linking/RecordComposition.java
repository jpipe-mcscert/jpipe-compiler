package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Records on a composed model the operator call that built it; see
 * {@link JustificationModel#recordComposition(Composition)}.
 */
public final class RecordComposition extends RegularCommand {

	private final String container;
	private final String operator;
	private final List<String> sourceNames;
	private final Map<String, String> arguments;
	private final Map<String, Composition.Scope> idArguments;

	/**
	 * @param container
	 *            the composed model.
	 * @param operator
	 *            the operator's name.
	 * @param sourceNames
	 *            the models composed, in call order.
	 * @param arguments
	 *            the call's arguments, element ids as the unit names them.
	 * @param idArguments
	 *            the keys of {@code arguments} whose value holds element ids,
	 *            and where those ids name an element.
	 */
	public RecordComposition(String container, String operator,
			List<String> sourceNames, Map<String, String> arguments,
			Map<String, Composition.Scope> idArguments) {
		this.container = container;
		this.operator = operator;
		this.sourceNames = List.copyOf(sourceNames);
		this.arguments = Map.copyOf(arguments);
		this.idArguments = Map.copyOf(idArguments);
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container).isPresent() && sourceNames
				.stream().allMatch(name -> unit.findModel(name).isPresent());
	}

	@Override
	public void doExecute(Unit context) {
		List<JustificationModel> sources = sourceNames.stream()
				.map(context::get).toList();
		context.get(container).recordComposition(
				new Composition(operator, sources, arguments, idArguments));
	}

	@Override
	public Optional<String> target() {
		return Optional.of(container);
	}

	@Override
	public String toString() {
		return "recordComposition('" + container + "', '" + operator + "', "
				+ sourceNames + ").";
	}

	/** Created while the unit is built, never read from a file. */
	@Override
	public Command withNamespace(String ns) {
		throw new UnsupportedOperationException(
				"RecordComposition is never read from a file");
	}
}
