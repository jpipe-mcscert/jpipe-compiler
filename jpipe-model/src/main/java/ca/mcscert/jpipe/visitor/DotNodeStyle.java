package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/**
 * Visual properties of a single DOT node type, plus the palette used by
 * {@link DotExporter}.
 *
 * <p>
 * {@code dotStyle} is the DOT {@code style=} value (e.g. {@code "filled"} or
 * {@code "filled,rounded"}); {@code fillColor} maps to {@code fillcolor=};
 * {@code borderColor} maps to {@code color=}. Any field may be {@code null} to
 * omit that attribute. Hex color values (e.g. {@code "#0072B2"}) are quoted
 * automatically.
 *
 * <p>
 * Palette derived from Okabe-Ito (2008) — safe for deuteranopia, protanopia,
 * and tritanopia. Fills use lightened versions of the base hues so that black
 * text remains readable (WCAG AA).
 */
record DotNodeStyle(String shape, String dotStyle, String fillColor,
		String borderColor) {

	// -------------------------------------------------------------------------
	// Palette
	// -------------------------------------------------------------------------

	static final DotNodeStyle CONCLUSION = new DotNodeStyle("rect",
			"filled,rounded", "lightgrey", null);
	static final DotNodeStyle SUB_CONCLUSION = new DotNodeStyle("rect", null,
			null, "#0072B2"); // Okabe-Ito blue
	static final DotNodeStyle STRATEGY = new DotNodeStyle("hexagon", "filled",
			"#F0C27F", null); // amber
	static final DotNodeStyle EVIDENCE = new DotNodeStyle("note", "filled",
			"#9ECAE1", null); // sky blue
	static final DotNodeStyle ABSTRACT_SUPPORT = new DotNodeStyle("rect",
			"dotted", null, null);

	// -------------------------------------------------------------------------

	/** Returns the {@link DotNodeStyle} for the given element type. */
	static DotNodeStyle of(JustificationElement element) {
		return switch (element) {
			case Conclusion _ -> CONCLUSION;
			case SubConclusion _ -> SUB_CONCLUSION;
			case Strategy _ -> STRATEGY;
			case Evidence _ -> EVIDENCE;
			case AbstractSupport _ -> ABSTRACT_SUPPORT;
		};
	}

	// -------------------------------------------------------------------------

	/** Renders this style as a DOT attribute fragment. */
	String toAttrs() {
		StringBuilder sb = new StringBuilder("shape=").append(shape);
		if (dotStyle != null) {
			sb.append(", style=");
			if (dotStyle.contains(","))
				sb.append('"').append(dotStyle).append('"');
			else
				sb.append(dotStyle);
		}
		if (fillColor != null)
			sb.append(", fillcolor=").append(dotColor(fillColor));
		if (borderColor != null)
			sb.append(", color=").append(dotColor(borderColor));
		return sb.toString();
	}

	/** Wraps hex color values in quotes; leaves X11 named colors as-is. */
	private static String dotColor(String color) {
		return color.startsWith("#") ? "\"" + color + "\"" : color;
	}
}
