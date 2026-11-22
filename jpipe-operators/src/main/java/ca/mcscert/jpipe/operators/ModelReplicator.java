package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.util.QualifiedIds;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces a {@link List} of {@link Command}s that copy all elements and
 * support edges from a source model into a target model, without mutating the
 * source.
 *
 * <p>
 * The {@code prefix} parameter controls id qualification, following the same
 * convention as {@link ca.mcscert.jpipe.model.JustificationModel#inline}:
 * <ul>
 * <li>blank / null → keep original ids unchanged</li>
 * <li>non-blank → prepend {@code prefix:} to plain ids; already-qualified ids
 * (containing {@code :}) are kept as-is</li>
 * </ul>
 *
 * <p>
 * When used inside a {@link MergeFunction}, the caller is responsible for
 * registering aliases in the {@link AliasRegistry} — this utility does not
 * touch the registry.
 *
 * @deprecated nothing in jPipe uses it: the operators copy source elements in
 *             their own partition phase. To be removed in the next major
 *             release.
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public final class ModelReplicator {

	private ModelReplicator() {
	}

	/**
	 * Returns commands that replicate {@code source} into
	 * {@code targetModelName} with element ids qualified by {@code prefix}.
	 */
	public static List<Command> replicate(String targetModelName,
			JustificationModel source, String prefix) {
		List<Command> commands = new ArrayList<>();

		// Step 1 — element creation
		for (JustificationElement elem : source.allElements()) {
			commands.add(ElementCreationCommand.of(targetModelName,
					qualifiedId(elem.id(), prefix), elem,
					SourceLocation.UNKNOWN));
		}

		// Step 2 — support edges
		source.forEachSupportEdge((supported,
				supporter) -> commands.add(new AddSupport(targetModelName,
						qualifiedId(supported, prefix),
						qualifiedId(supporter, prefix))));

		return List.copyOf(commands);
	}

	private static String qualifiedId(String id, String prefix) {
		if (prefix == null || prefix.isBlank()) {
			return id;
		}
		return QualifiedIds.qualifyIfPlain(prefix, id);
	}
}
