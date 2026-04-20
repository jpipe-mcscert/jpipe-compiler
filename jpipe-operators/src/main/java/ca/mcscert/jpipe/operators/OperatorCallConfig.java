package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.SourceLocation;
import java.util.List;
import java.util.Map;

/**
 * Describes a single operator call: what result to produce, which operator to
 * invoke, the source models, call-site arguments, source location, and the
 * declared result kind.
 *
 * <p>
 * The compact constructor defensively copies {@code sourceNames} and
 * {@code arguments} so that the record is immutable even when built from
 * mutable collections.
 */
public record OperatorCallConfig(String resultName, String operatorName,
		List<String> sourceNames, Map<String, String> arguments,
		SourceLocation location, ModelKind declaredKind) {

	public OperatorCallConfig {
		sourceNames = List.copyOf(sourceNames);
		arguments = Map.copyOf(arguments);
	}
}
