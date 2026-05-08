package ca.mcscert.jpipe.operators.builtin;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.operators.EquivalenceRelation;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.operators.InvalidOperatorCallException;
import ca.mcscert.jpipe.operators.MergeFunction;
import ca.mcscert.jpipe.operators.SourcedElement;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in {@code refine} composition operator.
 *
 * <p>
 * Syntax:
 *
 * <pre>
 * justification result is refine(base, refinement) {
 *     hook: "elementId"
 * }
 * </pre>
 *
 * <p>
 * Semantics: the hook element (identified by its id in the first source model)
 * and the conclusion of the second source ({@code refinement}) are merged into
 * a single {@link SubConclusion} named {@code "hook"}. All other elements are
 * kept with their source model name as id prefix (e.g. {@code base:c},
 * {@code refinement:s}).
 *
 * <p>
 * The hook id is resolved through the unit alias map, so it can refer to an
 * element that was renamed by a prior composition step (e.g. unification after
 * {@code assemble}).
 */
public final class RefineOperator extends CompositionOperator {

	/** The fixed id of the merged SubConclusion in the result model. */
	public static final String HOOK_ID = "hook";

	@Override
	protected Set<String> requiredArguments() {
		return Set.of("hook");
	}

	@Override
	protected Command createResultModel(String name, SourceLocation location,
			List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		return new CreateJustification(name, location);
	}

	@Override
	protected EquivalenceRelation equivalenceRelation(
			List<JustificationModel<?>> sources, Map<String, String> arguments,
			Map<String, String> knownAliases) {
		if (sources.size() != 2) {
			throw new InvalidOperatorCallException(
					"refine requires exactly 2 sources, got " + sources.size());
		}
		JustificationModel<?> base = sources.get(0);
		JustificationModel<?> refinement = sources.get(1);
		String hookElementId = arguments.get("hook");
		// Resolve through the unit alias map: the element may have been renamed
		// by a prior composition step (e.g. unification after assemble).
		String resolvedHookId = knownAliases.getOrDefault(
				base.getName() + "/" + hookElementId, hookElementId);

		return (a, b) -> a instanceof SourcedElement sa
				&& b instanceof SourcedElement sb
				&& (isHookPair(sa, sb, base, resolvedHookId, refinement)
						|| isHookPair(sb, sa, base, resolvedHookId,
								refinement));
	}

	@Override
	protected MergeFunction mergeFunction(List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		JustificationModel<?> refinement = sources.get(1);

		return (resultName, group, aliases) -> {
			List<SourcedElement> members = group.members();
			if (members.size() == 1) {
				// Singleton: create element with source-name prefix.
				// No alias registration needed — Phase 2 derives the same
				// qualified id and resolve() returns it unchanged.
				SourcedElement se = members.get(0);
				String newId = se.source().getName() + ":" + se.element().id();
				return elementCommand(resultName, newId, se);
			} else {
				// Hook group: hook element + refinement conclusion →
				// SubConclusion.
				// Register qualified old ids so Phase 2 resolves both to
				// HOOK_ID.
				List<String> qualOldIds = members.stream().map(
						se -> se.source().getName() + ":" + se.element().id())
						.toList();
				aliases.register(HOOK_ID, qualOldIds);
				String label = labelFrom(members, refinement);
				return List.of(
						new CreateSubConclusion(resultName, HOOK_ID, label));
			}
		};
	}

	// ── Helpers
	// ───────────────────────────────────────────────────────────────

	/**
	 * Returns {@code true} if {@code hook} is the designated hook element and
	 * {@code refinement} is the conclusion of the refinement source.
	 */
	private static boolean isHookPair(SourcedElement hook,
			SourcedElement refinement, JustificationModel<?> base,
			String resolvedHookId, JustificationModel<?> refinementSource) {
		return hook.source() == base
				&& hook.element().id().equals(resolvedHookId)
				&& refinement.source() == refinementSource
				&& refinement.element() instanceof Conclusion;
	}

	/** Returns the label of the member that comes from {@code source}. */
	private static String labelFrom(List<SourcedElement> members,
			JustificationModel<?> source) {
		return members.stream().filter(se -> se.source() == source)
				.map(se -> se.element().label()).findFirst().orElseThrow();
	}

	/** Produces the appropriate creation command for a single element copy. */
	private static List<Command> elementCommand(String resultName, String newId,
			SourcedElement se) {
		JustificationElement e = se.element();
		SourceLocation loc = se.location();
		return switch (e) {
			case Conclusion c -> List.of(
					new CreateConclusion(resultName, newId, c.label(), loc));
			case Strategy s ->
				List.of(new CreateStrategy(resultName, newId, s.label(), loc));
			case Evidence ev ->
				List.of(new CreateEvidence(resultName, newId, ev.label(), loc));
			case SubConclusion sc -> List.of(new CreateSubConclusion(resultName,
					newId, sc.label(), loc));
			case AbstractSupport as ->
				List.of(new CreateAbstractSupport(resultName, newId, as.label(),
						loc));
		};
	}
}
