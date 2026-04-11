package ca.mcscert.jpipe.operators.builtin;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.operators.AliasRegistry;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.operators.EquivalenceRelation;
import ca.mcscert.jpipe.operators.MergeFunction;
import ca.mcscert.jpipe.operators.SourcedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in {@code assemble} composition operator.
 *
 * <p>
 * Syntax:
 *
 * <pre>
 * justification assembled is assemble(a, b, c) {
 *     conclusionLabel: "A global conclusion"
 *     strategyLabel:   "An aggregating strategy"
 * }
 * </pre>
 *
 * <p>
 * Semantics: each source model's conclusion is demoted to a
 * {@link SubConclusion} (preserving its label and source-prefixed id). All
 * demoted sub-conclusions feed a newly created {@link Strategy} (id
 * {@value #STRATEGY_ID}), which in turn supports a newly created
 * {@link Conclusion} (id {@value #CONCLUSION_ID}). All other elements are
 * copied with {@code sourceName:elementId} prefixed ids.
 *
 * <p>
 * If any source is a {@link Template}, the result model is also a
 * {@link Template}; otherwise it is a {@code Justification}.
 */
public final class AssembleOperator extends CompositionOperator {

	/** Id of the synthesized aggregating strategy in the result model. */
	public static final String STRATEGY_ID = "assembleStrategy";

	/** Id of the synthesized global conclusion in the result model. */
	public static final String CONCLUSION_ID = "assembleConclusion";

	@Override
	protected Set<String> requiredArguments() {
		return Set.of("conclusionLabel", "strategyLabel");
	}

	@Override
	protected Command createResultModel(String name, SourceLocation location,
			List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		boolean anyTemplate = sources.stream()
				.anyMatch(Template.class::isInstance);
		return anyTemplate
				? new CreateTemplate(name, location)
				: new CreateJustification(name, location);
	}

	@Override
	protected EquivalenceRelation equivalenceRelation(
			List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		return (a, b) -> false; // no merging — every element is its own group
	}

	@Override
	protected MergeFunction mergeFunction(List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		return (resultName, group, aliases) -> {
			SourcedElement se = group.members().get(0);
			JustificationElement e = se.element();
			String newId = se.source().getName() + ":" + e.id();
			SourceLocation loc = se.location();
			return switch (e) {
				// Conclusions are demoted to SubConclusions
				case Conclusion c -> List.of(new CreateSubConclusion(resultName,
						newId, c.label(), loc));
				case Strategy s -> List.of(
						new CreateStrategy(resultName, newId, s.label(), loc));
				case Evidence ev -> List.of(
						new CreateEvidence(resultName, newId, ev.label(), loc));
				case SubConclusion sc ->
					List.of(new CreateSubConclusion(resultName, newId,
							sc.label(), loc));
				case AbstractSupport as ->
					List.of(new CreateAbstractSupport(resultName, newId,
							as.label(), loc));
			};
		};
	}

	@Override
	protected List<Command> additionalCommands(String resultName,
			List<JustificationModel<?>> sources, AliasRegistry aliases,
			Map<String, String> args) {
		List<Command> commands = new ArrayList<>();

		// Synthesized aggregating elements
		commands.add(new CreateStrategy(resultName, STRATEGY_ID,
				args.get("strategyLabel")));
		commands.add(new CreateConclusion(resultName, CONCLUSION_ID,
				args.get("conclusionLabel")));

		// Each demoted conclusion supports the new aggregating strategy
		for (JustificationModel<?> source : sources) {
			source.conclusion()
					.ifPresent(c -> commands.add(new AddSupport(resultName,
							STRATEGY_ID, source.getName() + ":" + c.id())));
		}

		// New strategy supports new conclusion
		commands.add(new AddSupport(resultName, CONCLUSION_ID, STRATEGY_ID));

		return commands;
	}
}
