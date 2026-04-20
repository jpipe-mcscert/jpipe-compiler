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
