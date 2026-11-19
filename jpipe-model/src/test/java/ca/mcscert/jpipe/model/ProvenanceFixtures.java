package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Models built by hand as the compiler builds them, for tests that read ids
 * through the models they come from.
 */
public final class ProvenanceFixtures {

	private ProvenanceFixtures() {
	}

	/**
	 * The models of #202: {@code ns:a} and {@code b:ns} each hold an evidence
	 * {@code e}, {@code b} assembles {@code ns:a} and {@code k}, and
	 * {@code out} assembles {@code b} and {@code b:ns}. In {@code out},
	 * {@code b:ns:a:e} is {@code (b)(ns:a)(e)}, never {@code (b:ns)(a)(e)}.
	 */
	public static Map<String, JustificationModel> nestedNamespaces() {
		Justification nsA = leaf("ns:a", "e");
		Justification k = leaf("k", "e");
		Justification bNs = leaf("b:ns", "e");
		Justification b = composed("b", List.of(nsA, k), Map.of());
		Justification out = composed("out", List.of(b, bNs),
				Map.of("unifyExclude", "b:ns:a:e"));
		return Map.of("ns:a", nsA, "k", k, "b:ns", bNs, "b", b, "out", out);
	}

	/**
	 * The sources of {@code refine(base, ns) { hook: "ns:a:e" }}: {@code base}
	 * holds {@code ns:a:e}, taken from {@code ns:a}, and {@code ns} holds
	 * {@code a:e}, taken from {@code a}. The hook names an element of
	 * {@code base}, as {@code base} names it; read as an id of the result, it
	 * would be {@code ns}'s {@code a:e} instead.
	 */
	public static Map<String, JustificationModel> hookScope() {
		Justification nsA = leaf("ns:a", "e");
		Justification a = leaf("a", "e");
		return Map.of("ns:a", nsA, "a", a, "base",
				composed("base", List.of(nsA), Map.of()), "ns",
				composed("ns", List.of(a), Map.of()));
	}

	/**
	 * {@code base} holds {@code extra:b:foo:a:e}, through three compositions:
	 * {@code extra} of {@code b}, {@code b} of {@code foo:a}. {@code refine}
	 * accepts {@code b:foo:a:e} for it, the short form
	 * {@link JustificationModel#findById} resolves.
	 */
	public static Map<String, JustificationModel> suffixChain() {
		Justification fooA = leaf("foo:a", "e");
		Justification b = composed("b", List.of(fooA), Map.of());
		Justification extra = composed("extra", List.of(b), Map.of());
		return Map.of("foo:a", fooA, "b", b, "extra", extra, "base",
				composed("base", List.of(extra), Map.of()));
	}

	/**
	 * Two models whose ids collide once composed: {@code b} holds
	 * {@code ns:a:e}, taken from {@code ns:a}, and {@code b:ns} holds
	 * {@code a:e}, the override of the template {@code a}'s {@code @support}.
	 * In {@code out}, which composes both, {@code b:ns:a:e} names either.
	 */
	public static JustificationModel collision() {
		Justification b = composed("b", List.of(leaf("ns:a", "e")), Map.of());
		Template a = new Template("a");
		a.setConclusion(new Conclusion("c", "A holds"));
		Strategy s = new Strategy("s", "Check A");
		AbstractSupport e = new AbstractSupport("e", "Some evidence");
		s.addSupport(e);
		a.addElement(s);
		a.addElement(e);
		Justification bNs = new Justification("b:ns");
		bNs.inline(a, "a");
		return composed("out", List.of(b, bNs), Map.of());
	}

	/**
	 * A two-level chain: {@code root} declares {@code c}, {@code s} and the
	 * {@code @support abs}; {@code base:mid} implements it, overriding
	 * {@code abs} and adding {@code m}; {@code leaf} implements
	 * {@code base:mid}.
	 */
	public static JustificationModel templateChain() {
		Template root = new Template("root");
		root.setConclusion(new Conclusion("c", "Root holds"));
		Strategy s = new Strategy("s", "Check root");
		AbstractSupport abs = new AbstractSupport("abs", "Some support");
		s.addSupport(abs);
		root.addElement(s);
		root.addElement(abs);

		Template mid = new Template("base:mid");
		mid.inline(root, "root");
		mid.removeElement("root:abs");
		mid.addElement(new Evidence("root:abs", "Mid evidence"));
		mid.addElement(new Evidence("m", "Mid's own"));

		Justification leaf = new Justification("leaf");
		leaf.inline(mid, "base:mid");
		return leaf;
	}

	private static Justification leaf(String name, String evidenceId) {
		Justification j = new Justification(name);
		Conclusion c = new Conclusion("c", name + " holds");
		Strategy s = new Strategy("s", "Check " + name);
		Evidence e = new Evidence(evidenceId, "Evidence of " + name);
		c.addSupport(s);
		s.addSupport(e);
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(e);
		return j;
	}

	/**
	 * {@code name}, holding every element of {@code sources} as
	 * {@code source:id}, as {@code assemble} copies them, plus the conclusion
	 * it creates.
	 */
	private static Justification composed(String name,
			List<JustificationModel> sources, Map<String, String> arguments) {
		Justification j = new Justification(name);
		j.setConclusion(new Conclusion("assembleConclusion", name + " holds"));
		for (JustificationModel source : sources) {
			source.allElements().forEach(e -> j.addElement(
					new Evidence(source.getName() + ":" + e.id(), e.label())));
		}
		j.recordComposition(new Composition("assemble", sources, arguments,
				Set.of("unifyExclude")));
		return j;
	}
}
