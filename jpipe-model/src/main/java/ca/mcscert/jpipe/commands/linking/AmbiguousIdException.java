package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A plain id in a relation names several elements a model inherits, such as
 * {@code a} when both {@code root:a} and {@code mid:a} are inherited. Picking
 * one would silently bind the relation to an element the author may not have
 * meant (ADR-0012, #177).
 */
public final class AmbiguousIdException extends RuntimeException {

	public AmbiguousIdException(String id, String container,
			List<JustificationElement> matches) {
		super("ambiguous id '" + id + "' in model '" + container
				+ "': it names " + matches.stream().map(e -> "'" + e.id() + "'")
						.collect(Collectors.joining(", "))
				+ "; write the qualified id");
	}
}
