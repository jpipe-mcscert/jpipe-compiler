package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import org.junit.jupiter.api.Test;

class DotExporterTest {

	@Test
	void export_nodeIdsAreQualifiedWithModelName() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "System correct"));
		j.addElement(new Strategy("s", "Testing"));
		j.addElement(new Evidence("e1", "Test results"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("id=\"j:c\"");
		assertThat(dot).contains("id=\"j:s\"");
		assertThat(dot).contains("id=\"j:e1\"");
	}

	@Test
	void export_shortLabelIsNotWrapped() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "System correct"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("label=\"System correct\"");
	}

	@Test
	void export_longLabelIsWrappedAtWordBoundary() {
		Justification j = new Justification("j");
		// 46 chars — exceeds the 40-char wrap width
		j.setConclusion(new Conclusion("c",
				"The system is fully correct and validated"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains(
				"label=\"The system is fully correct and\\nvalidated\"");
	}

	@Test
	void export_labelSpecialCharsAreEscapedBeforeWrapping() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A \"quoted\" label"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("label=\"A \\\"quoted\\\" label\"");
	}

	@Test
	void export_templateExpandedElementsCarryModelAndTemplatePrefix() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("t:c", "System correct"));
		j.addElement(new Strategy("t:s", "Testing"));
		j.addElement(new Evidence("t:e1", "Test results"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("id=\"j:t:c\"");
		assertThat(dot).contains("id=\"j:t:s\"");
		assertThat(dot).contains("id=\"j:t:e1\"");
	}
}
