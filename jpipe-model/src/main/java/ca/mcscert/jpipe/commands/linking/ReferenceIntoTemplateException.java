package ca.mcscert.jpipe.commands.linking;

/**
 * Raised when a model implementing a template references a template-internal
 * element (a strategy or conclusion) instead of only overriding the template's
 * {@code @support} placeholders. Carries a ready-to-report message; the
 * compiler layer maps it to a located diagnostic.
 */
public final class ReferenceIntoTemplateException extends RuntimeException {

	public ReferenceIntoTemplateException(String referencedId,
			String container) {
		super("cannot reference template-internal element '" + referencedId
				+ "' from '" + container
				+ "'; a model implementing a template may only override its"
				+ " @support placeholders");
	}
}
