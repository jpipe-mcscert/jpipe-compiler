package ca.mcscert.jpipe.model;

/**
 * A single rule violation found during consistency or completeness validation.
 *
 * <p>
 * {@code rule} is a stable kebab-case identifier (e.g.
 * {@code "no-duplicate-ids"}) that callers can use for filtering or
 * localisation. {@code message} is a human-readable description.
 * {@code location} is the source position of the offending element, or
 * {@link SourceLocation#UNKNOWN} when the model was built programmatically
 * without location tracking.
 */
public record Violation(String rule, String message, SourceLocation location) {
}
