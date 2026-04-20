package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadResolverPrefixTest {

	@Test
	void prefixesJustificationIdentifier() {
		var cmd = new CreateJustification("foo");
		var result = prefixOne("ns", cmd);
		assertThat(((CreateJustification) result).identifier())
				.isEqualTo("ns:foo");
	}

	@Test
	void prefixesTemplateIdentifier() {
		var cmd = new CreateTemplate("tmpl");
		var result = prefixOne("lib", cmd);
		assertThat(((CreateTemplate) result).identifier())
				.isEqualTo("lib:tmpl");
	}

	@Test
	void prefixesConclusionContainerButNotElementIdOrLabel() {
		var cmd = new CreateConclusion("myModel", "c", "My label");
		var result = (CreateConclusion) prefixOne("ns", cmd);
		assertThat(result.container()).isEqualTo("ns:myModel");
		assertThat(result.identifier()).isEqualTo("c");
		assertThat(result.label()).isEqualTo("My label");
	}

	@Test
	void prefixesEvidenceContainerButNotElementId() {
		var cmd = new CreateEvidence("justification1", "e1", "Evidence label");
		var result = (CreateEvidence) prefixOne("ns", cmd);
		assertThat(result.container()).isEqualTo("ns:justification1");
		assertThat(result.identifier()).isEqualTo("e1");
		assertThat(result.label()).isEqualTo("Evidence label");
	}

	@Test
	void prefixesImplementsTemplateBothModelNames() {
		var cmd = new ImplementsTemplate("child", "parent");
		var result = (ImplementsTemplate) prefixOne("ns", cmd);
		assertThat(result.modelName()).isEqualTo("ns:child");
		assertThat(result.templateName()).isEqualTo("ns:parent");
	}

	@Test
	void prefixesAddSupportContainerOnly() {
		var cmd = new AddSupport("model1", "from_elem", "to_elem");
		var result = (AddSupport) prefixOne("ns", cmd);
		assertThat(result.container()).isEqualTo("ns:model1");
		assertThat(result.supportableId()).isEqualTo("from_elem");
		assertThat(result.supporterId()).isEqualTo("to_elem");
	}

	@Test
	void returnsLoadDirectiveUnchanged() {
		var cmd = new LoadResolver.LoadDirective("some/path.jd", "alias");
		var result = prefixOne("ns", cmd);
		assertThat(result).isSameAs(cmd);
	}

	@Test
	void prefixesAllCommandsInList() {
		List<Command> commands = List.of(new CreateTemplate("t"),
				new CreateJustification("j"), new ImplementsTemplate("j", "t"));
		var result = LoadResolver.prefix("x", commands);
		assertThat(result).hasSize(3);
		assertThat(((CreateTemplate) result.get(0)).identifier())
				.isEqualTo("x:t");
		assertThat(((CreateJustification) result.get(1)).identifier())
				.isEqualTo("x:j");
		assertThat(((ImplementsTemplate) result.get(2)).modelName())
				.isEqualTo("x:j");
		assertThat(((ImplementsTemplate) result.get(2)).templateName())
				.isEqualTo("x:t");
	}

	private static Command prefixOne(String ns, Command cmd) {
		return LoadResolver.prefix(ns, List.of(cmd)).get(0);
	}
}
