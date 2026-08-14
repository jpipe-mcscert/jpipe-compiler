package ca.mcscert.jpipe.model.elements;

/**
 * A placeholder element in a template, to be substituted when the template is
 * implemented.
 */
public record AbstractSupport(String id,
		String label) implements JustificationElement, SupportLeaf {

	@Override
	public String kind() {
		return "abstract-support";
	}
}
