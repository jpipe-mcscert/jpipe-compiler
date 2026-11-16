package ca.mcscert.jpipe.operators.builtin;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
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
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.operators.ModelKind;
import java.util.HashSet;
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
	public Set<String> elementArguments() {
		return Set.of(HOOK_ID);
	}

	/**
	 * A template when the result keeps an abstract support, that is, when a
	 * source holds one other than the hook, which the refinement replaces; a
	 * justification otherwise. A refine could only produce a justification, so
	 * refining a template that keeps a {@code @support} failed (#181).
	 */
	@Override
	public ModelKind resultKind(List<JustificationModel> sources,
			Map<String, String> args) {
		String hook = args.get(HOOK_ID);
		JustificationModel base = sources.isEmpty() ? null : sources.getFirst();
		boolean keepsAPlaceholder = sources.stream()
				.anyMatch(source -> source.elementsOfType(AbstractSupport.class)
						.stream().anyMatch(as -> !(source == base
								&& hook != null && names(as, hook))));
		return keepsAPlaceholder ? ModelKind.TEMPLATE : ModelKind.JUSTIFICATION;
	}

	/** Whether {@code id}, plain or qualified, names {@code element}. */
	private static boolean names(JustificationElement element, String id) {
		return element.id().equals(id) || element.id().endsWith(":" + id);
	}

	@Override
	protected Command createResultModel(String name, SourceLocation location,
			List<JustificationModel> sources, Map<String, String> arguments) {
		return resultKind(sources, arguments) == ModelKind.TEMPLATE
				? new CreateTemplate(name, location)
				: new CreateJustification(name, location);
	}

	@Override
	protected EquivalenceRelation equivalenceRelation(
			List<JustificationModel> sources, Map<String, String> arguments,
			Map<String, String> knownAliases) {
		if (sources.size() != 2) {
			throw new InvalidOperatorCallException(
					"refine requires exactly 2 sources, got " + sources.size());
		}
		JustificationModel base = sources.get(0);
		JustificationModel refinement = sources.get(1);
		String resolvedHookId = resolveHook(base, arguments.get(HOOK_ID),
				knownAliases).id();

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
	 * The element of {@code base} that {@code hook} names. Through the unit's
	 * aliases, followed to the end of the chain, since an element can have been
	 * renamed by several composition steps; then by id, or by the one inherited
	 * element whose qualified id ends with it. It must be an evidence or an
	 * abstract support: only a leaf can be refined (#181).
	 */
	static JustificationElement resolveHook(JustificationModel base,
			String hook, Map<String, String> knownAliases) {
		String current = hook;
		Set<String> visited = new HashSet<>();
		while (visited.add(current)) {
			String next = knownAliases
					.get(Unit.registryKey(base.getName(), current));
			if (next == null) {
				break;
			}
			current = next;
		}
		String id = current;
		JustificationElement element = base.allElements().stream()
				.filter(e -> e.id().equals(id)).findFirst()
				.orElseGet(() -> uniqueMatch(base, hook, id));
		if (!(element instanceof Evidence
				|| element instanceof AbstractSupport)) {
			throw new InvalidOperatorCallException("hook '" + hook + "' is a "
					+ kindOf(element) + " of '" + base.getName()
					+ "': only an evidence or a @support can be refined");
		}
		return element;
	}

	/** The one inherited element {@code id} names, or a clear refusal. */
	private static JustificationElement uniqueMatch(JustificationModel base,
			String hook, String id) {
		List<JustificationElement> matches = base.inheritedMatches(id);
		if (matches.isEmpty()) {
			throw new InvalidOperatorCallException("hook element '" + hook
					+ "' not found in base model '" + base.getName() + "'");
		}
		if (matches.size() > 1) {
			throw new InvalidOperatorCallException("ambiguous hook '" + hook
					+ "' in '" + base.getName() + "': it names "
					+ matches.stream().map(e -> "'" + e.id() + "'")
							.collect(Collectors.joining(", "))
					+ "; write the qualified id");
		}
		return matches.getFirst();
	}

	private static String kindOf(JustificationElement element) {
		return switch (element) {
			case Conclusion _ -> "conclusion";
			case SubConclusion _ -> "sub-conclusion";
			case Strategy _ -> "strategy";
			default -> element.getClass().getSimpleName();
		};
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
