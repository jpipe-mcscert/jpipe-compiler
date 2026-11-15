package ca.mcscert.jpipe.visitor;

/**
 * Thrown by an exporter that refuses to write a model in which one identifier
 * designates two elements, since the artefact would silently drop one of them.
 *
 * <p>
 * The consistency rules ({@code no-duplicate-ids}, {@code unique-identifiers})
 * report the cause as an error, but an error does not stop the export. Callers
 * that drive an exporter on a model with diagnosed errors catch this type and
 * report it as an ordinary failure of the export, rather than as a bug.
 */
public final class AmbiguousIdentifierException extends IllegalStateException {

	public AmbiguousIdentifierException(String message) {
		super(message);
	}
}
