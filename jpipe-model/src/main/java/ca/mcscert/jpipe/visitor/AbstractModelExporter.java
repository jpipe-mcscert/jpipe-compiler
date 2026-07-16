package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base for exporters that serialise a single
 * {@link JustificationModel} to text. Provides the common infrastructure shared
 * by all single-model exporters:
 *
 * <ul>
 * <li>A {@link StringBuilder} accumulator ({@link #builder}) and the associated
 * reset-before-export pattern.
 * <li>A {@link #currentModelName} field populated from the model under export.
 * <li>A {@link #qualify(String)} helper that prefixes element ids with the
 * current model name ({@code "modelName:elementId"}).
 * <li>{@link #visit(Unit)} — rejects {@link Unit} with
 * {@link UnsupportedOperationException}; single-model exporters require
 * {@code SelectModel} to extract a model first.
 * <li>{@link #visit(Justification)} and {@link #visit(Template)} — both
 * delegate to {@link #exportModel(JustificationModel)}.
 * </ul>
 *
 * <p>
 * Subclasses implement {@link #exportModel(JustificationModel)} to perform the
 * actual serialisation, setting {@link #currentModelName} as their first
 * action. Element visit methods ({@code visit(Conclusion)}, etc.) are left
 * abstract so each exporter controls its own output format.
 */
public abstract class AbstractModelExporter
		implements
			JustificationVisitor<Void> {

	/** Accumulates the serialised output. Reset at the start of each export. */
	protected final StringBuilder builder = new StringBuilder();

	/**
	 * Name of the model currently being exported. Set by
	 * {@link #exportModel(JustificationModel)} before any element visit methods
	 * are called.
	 */
	protected String currentModelName;

	/**
	 * Inverted alias map for the model currently being exported: merged element
	 * id &rarr; the list of original ids that were merged into it. Populated by
	 * {@link #initAliases(JustificationModel)}.
	 */
	private final Map<String, List<String>> aliasesByTarget = new HashMap<>();

	/**
	 * Qualifies {@code elementId} with the current model name:
	 * {@code "currentModelName:elementId"}.
	 */
	protected final String qualify(String elementId) {
		return currentModelName + ":" + elementId;
	}

	/**
	 * Builds the inverted alias lookup for {@code model}. Subclasses that
	 * surface aliases must call this at the start of
	 * {@link #exportModel(JustificationModel)}, after setting
	 * {@link #currentModelName}.
	 */
	protected final void initAliases(JustificationModel<?> model) {
		aliasesByTarget.clear();
		model.aliases().forEach((oldId, newId) -> aliasesByTarget
				.computeIfAbsent(newId, _ -> new ArrayList<>()).add(oldId));
	}

	/**
	 * Returns the qualified original ids that were merged into
	 * {@code plainElementId}, or an empty list when the element is not the
	 * target of any alias. Requires {@link #initAliases(JustificationModel)} to
	 * have been called first.
	 */
	protected final List<String> qualifiedAliasesOf(String plainElementId) {
		List<String> originals = aliasesByTarget.get(plainElementId);
		if (originals == null) {
			return List.of();
		}
		return originals.stream().map(this::qualify).toList();
	}

	/**
	 * Rejects {@link Unit} — single-model exporters require a specific model.
	 * Use {@code SelectModel} in the compilation pipeline to extract one first.
	 */
	@Override
	public final Void visit(Unit unit) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " operates on a single model"
						+ " — use SelectModel to extract one from a Unit");
	}

	/** Delegates to {@link #exportModel(JustificationModel)}. */
	@Override
	public final Void visit(Justification justification) {
		exportModel(justification);
		return null;
	}

	/** Delegates to {@link #exportModel(JustificationModel)}. */
	@Override
	public final Void visit(Template template) {
		exportModel(template);
		return null;
	}

	/**
	 * Performs the actual serialisation of {@code model}. Implementations must
	 * set {@link #currentModelName}{@code = model.getName()} before calling any
	 * element visit methods.
	 */
	protected abstract void exportModel(JustificationModel<?> model);
}
