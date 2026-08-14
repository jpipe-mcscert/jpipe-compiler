package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;

/** Shared model builders for visitor/exporter tests. */
class ModelFixtures {

	private ModelFixtures() {
	}

	/**
	 * Justification with a single Conclusion &larr; Strategy &larr; Evidence
	 * chain.
	 *
	 * <ul>
	 * <li>name: {@code "j"}
	 * <li>Conclusion {@code "c"} &mdash; "The system is correct"
	 * <li>Strategy {@code "s"} &mdash; "Testing"
	 * <li>Evidence {@code "e1"} &mdash; "Test results"
	 * </ul>
	 */
	static Justification simpleJustification() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "The system is correct");
		Strategy s = new Strategy("s", "Testing");
		Evidence e = new Evidence("e1", "Test results");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(e);
		s.addSupport(e);
		c.addSupport(s);
		return j;
	}

	/**
	 * A {@link #simpleJustification()} whose strategy {@code "s"} is the merge
	 * target of two original ids ({@code "a:s"} and {@code "b:s"}), as recorded
	 * during composition/unification.
	 */
	static Justification unifiedJustification() {
		Justification j = simpleJustification();
		j.recordAlias("a:s", "s");
		j.recordAlias("b:s", "s");
		return j;
	}

	/**
	 * A {@link #simpleJustification()} whose strategy {@code "s"} is the merge
	 * target of a two-level chain, as produced by composing the result of a
	 * composition: {@code outer:a:s1} and {@code outer:b:s2} were first merged
	 * into {@code outer:s}, which a later composition merged into {@code "s"}.
	 *
	 * <p>
	 * The leaves are deliberately given distinct short ids, so that resolving
	 * the chain one hop short, stopping at {@code outer:s}, produces visibly
	 * different output from resolving it all the way.
	 *
	 * <p>
	 * The recording order mirrors the compiler's: the carry-forward aliases of
	 * phases 1&ndash;3 are emitted before the unification alias of phase 4.
	 */
	static Justification chainedAliasJustification() {
		Justification j = simpleJustification();
		j.recordAlias("outer:a:s1", "outer:s");
		j.recordAlias("outer:b:s2", "outer:s");
		j.recordAlias("outer:s", "s");
		return j;
	}

	/**
	 * A {@link #simpleJustification()} whose strategy {@code "s"} was merged
	 * from a group in which a unification id sits <em>beside</em> the ids it
	 * stood for rather than above them.
	 *
	 * <p>
	 * This is the shape {@code refine} produces over an already-composed
	 * source: the hook merge aliases {@code outer:unified_0} to the result, and
	 * the carry-forward then resolves that source's own originals to the same
	 * result, so all three become siblings. {@code outer:unified_0} is left
	 * with no originals of its own, so being childless does not make an id
	 * authored, and only {@link Justification#recordUnifiedId(String)} tells
	 * them apart.
	 */
	static Justification siblingUnifiedIdJustification() {
		Justification j = simpleJustification();
		j.recordAlias("outer:unified_0", "s");
		j.recordAlias("outer:a:s1", "s");
		j.recordAlias("outer:b:s2", "s");
		j.recordUnifiedId("outer:unified_0");
		return j;
	}

	/**
	 * A justification whose two evidences share the plain id {@code "e"} under
	 * different source-model prefixes, so that neither can be linked by
	 * {@code "e"} alone.
	 *
	 * <ul>
	 * <li>name: {@code "j"}
	 * <li>Conclusion {@code "c"}, Strategy {@code "s"}
	 * <li>Evidence {@code "a:e"} &mdash; "First", Evidence {@code "b:e"}
	 * &mdash; "Second"
	 * </ul>
	 */
	static Justification ambiguousShortIdJustification() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "The system is correct");
		Strategy s = new Strategy("s", "Testing");
		Evidence first = new Evidence("a:e", "First");
		Evidence second = new Evidence("b:e", "Second");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(first);
		j.addElement(second);
		s.addSupport(first);
		s.addSupport(second);
		c.addSupport(s);
		return j;
	}

	/**
	 * Template with a single Conclusion &larr; Strategy &larr; AbstractSupport
	 * chain.
	 *
	 * <ul>
	 * <li>name: {@code "t"}
	 * <li>Conclusion {@code "c"} &mdash; "Conclusion"
	 * <li>Strategy {@code "s"} &mdash; "Strategy"
	 * <li>AbstractSupport {@code "abs"} &mdash; "Abstract step"
	 * </ul>
	 */
	static Template simpleTemplate() {
		Template t = new Template("t");
		Conclusion c = new Conclusion("c", "Conclusion");
		Strategy s = new Strategy("s", "Strategy");
		AbstractSupport abs = new AbstractSupport("abs", "Abstract step");
		t.setConclusion(c);
		t.addElement(s);
		t.addElement(abs);
		s.addSupport(abs);
		c.addSupport(s);
		return t;
	}
}
