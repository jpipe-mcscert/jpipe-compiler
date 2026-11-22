package ca.mcscert.jpipe.operators.builtin;

import ca.mcscert.jpipe.model.Binding;
import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.model.Composition.Scope;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.operators.EquivalenceRelation;
import ca.mcscert.jpipe.operators.InvalidOperatorCallException;
import ca.mcscert.jpipe.operators.MergeFunction;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.SourcedElement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import java.util.stream.Collectors;

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
		return Set.of(HOOK_ID);
	}

	@Override
	protected Map<String, Scope> idArguments() {
		return Map.of(HOOK_ID, Scope.SOURCE);
	}

	/**
	 * A template when the result keeps an abstract support, one the hook did
	 * not replace; a justification otherwise. Decided from the elements the
	 * composition built, so it cannot disagree with the hook it refined: a
	 * refine could only produce a justification, so refining a template that
	 * keeps a {@code @support} failed (#181).
	 */
	@Override
	protected ModelKind resultKind(List<JustificationModel> sources,
			Map<String, String> arguments, List<Command> elements) {
		return elements.stream()
				.anyMatch(CreateAbstractSupport.class::isInstance)
						? ModelKind.TEMPLATE
						: ModelKind.JUSTIFICATION;
	}

	@Override
	protected EquivalenceRelation equivalenceRelation(
			List<JustificationModel> sources, Map<String, String> arguments) {
		if (sources.size() != 2) {
			throw new InvalidOperatorCallException(
					"refine requires exactly 2 sources, got " + sources.size());
		}
		JustificationModel base = sources.get(0);
		JustificationModel refinement = sources.get(1);
		String resolvedHookId = resolveHook(base, arguments.get(HOOK_ID)).id();

		return (a, b) -> a instanceof SourcedElement sa
				&& b instanceof SourcedElement sb
				&& (isHookPair(sa, sb, base, resolvedHookId, refinement)
						|| isHookPair(sb, sa, base, resolvedHookId,
								refinement));
	}

	@Override
	protected MergeFunction mergeFunction(List<JustificationModel> sources,
			Map<String, String> arguments) {
		JustificationModel refinement = sources.get(1);

		return (resultName, group, aliases) -> {
			List<SourcedElement> members = group.members();
			if (members.size() == 1) {
				// Singleton: create element with source-name prefix.
				// No alias registration needed — Phase 2 derives the same
				// qualified id and resolve() returns it unchanged.
				SourcedElement se = members.getFirst();
				return List.of(ElementCreationCommand.of(resultName,
						se.qualifiedId(), se.element(), se.location()));
			} else {
				// Hook group: hook element + refinement conclusion →
				// SubConclusion.
				// Register qualified old ids so Phase 2 resolves both to
				// HOOK_ID.
				List<String> qualOldIds = members.stream()
						.map(SourcedElement::qualifiedId).toList();
				aliases.register(HOOK_ID, qualOldIds);
				String label = labelFrom(members, refinement);
				return List.of(
						new CreateSubConclusion(resultName, HOOK_ID, label));
			}
		};
	}

	/**
	 * The element of {@code base} that {@code hook} names: through the base's
	 * aliases, followed to the end of the chain, since an element can have been
	 * renamed by several composition steps, then by id, or by the one inherited
	 * element whose qualified id ends with it. It must be an evidence or an
	 * abstract support: only a leaf can be refined (#181).
	 */
	static JustificationElement resolveHook(JustificationModel base,
			String hook) {
		// A model's aliases form chains, one link per composition step, and
		// exporters read them as such (an id that is the target of an alias
		// is an intermediate merge result): followed to the end here.
		String id = base.resolveAlias(hook);
		JustificationElement element = switch (base.bind(id)) {
			case Binding.Found(JustificationElement found) -> found;
			case Binding.Ambiguous(List<JustificationElement> candidates) ->
				throw new InvalidOperatorCallException("ambiguous hook '" + hook
						+ "' in '" + base.getName() + "': it names "
						+ candidates.stream().map(e -> "'" + e.id() + "'")
								.collect(Collectors.joining(", "))
						+ "; write the qualified id");
			case Binding.Missing() ->
				throw new InvalidOperatorCallException("hook element '" + hook
						+ "' not found in base model '" + base.getName() + "'");
		};
		if (!(element instanceof Evidence
				|| element instanceof AbstractSupport)) {
			throw new InvalidOperatorCallException("hook '" + hook + "' is a "
					+ element.kind() + " of '" + base.getName()
					+ "': only an evidence or a @support can be refined");
		}
		return element;
	}

	// ── Helpers
	// ───────────────────────────────────────────────────────────────

	/**
	 * Returns {@code true} if {@code hook} is the designated hook element and
	 * {@code refinement} is the conclusion of the refinement source.
	 */
	private static boolean isHookPair(SourcedElement hook,
			SourcedElement refinement, JustificationModel base,
			String resolvedHookId, JustificationModel refinementSource) {
		return hook.source() == base
				&& hook.element().id().equals(resolvedHookId)
				&& refinement.source() == refinementSource
				&& refinement.element() instanceof Conclusion;
	}

	/** Returns the label of the member that comes from {@code source}. */
	private static String labelFrom(List<SourcedElement> members,
			JustificationModel source) {
		return members.stream().filter(se -> se.source() == source)
				.map(se -> se.element().label()).findFirst().orElseThrow();
	}
}
