package ca.mcscert.jpipe.model.elements;

/**
 * Sealed interface for elements that can appear in both justifications and
 * templates. Permits only {@link Evidence}, {@link Strategy},
 * {@link Conclusion}, and {@link SubConclusion}.
 */
public sealed interface CommonElement extends JustificationElement
		permits Evidence, Strategy, Conclusion, SubConclusion {
}
