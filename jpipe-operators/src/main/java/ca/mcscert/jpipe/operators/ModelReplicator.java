package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
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
 */
public final class ModelReplicator {

	private ModelReplicator() {
	}

	/**
	 * Returns commands that replicate {@code source} into
	 * {@code targetModelName} with element ids qualified by {@code prefix}.
	 */
	public static List<Command> replicate(String targetModelName,
			JustificationModel<?> source, String prefix) {
		List<Command> commands = new ArrayList<>();

		// Step 1 — element creation
		source.conclusion().ifPresent(c -> {
			String id = qualifiedId(c.id(), prefix);
			commands.add(new CreateConclusion(targetModelName, id, c.label()));
		});
		for (JustificationElement elem : source.getElements()) {
			String id = qualifiedId(elem.id(), prefix);
			commands.add(switch (elem) {
				case Strategy s ->
					new CreateStrategy(targetModelName, id, s.label());
				case Evidence e ->
					new CreateEvidence(targetModelName, id, e.label());
				case SubConclusion sc ->
					new CreateSubConclusion(targetModelName, id, sc.label());
				case AbstractSupport as ->
					new CreateAbstractSupport(targetModelName, id, as.label());
				default -> throw new IllegalStateException(
						"Unhandled element type: " + elem);
			});
		}

		// Step 2 — support edges
		source.conclusion()
				.ifPresent(c -> c.getSupport()
						.ifPresent(s -> commands.add(new AddSupport(
								targetModelName, qualifiedId(c.id(), prefix),
								qualifiedId(s.id(), prefix)))));
		source.subConclusions()
				.forEach(sc -> sc.getSupport()
						.ifPresent(s -> commands.add(new AddSupport(
								targetModelName, qualifiedId(sc.id(), prefix),
								qualifiedId(s.id(), prefix)))));
		source.strategies().forEach(s -> s.getSupports().forEach(leaf -> {
			String leafId = ((JustificationElement) leaf).id();
			commands.add(new AddSupport(targetModelName,
					qualifiedId(s.id(), prefix), qualifiedId(leafId, prefix)));
		}));

		return List.copyOf(commands);
	}

	private static String qualifiedId(String id, String prefix) {
		if (prefix == null || prefix.isBlank()) {
			return id;
		}
		return id.contains(":") ? id : prefix + ":" + id;
	}
}
