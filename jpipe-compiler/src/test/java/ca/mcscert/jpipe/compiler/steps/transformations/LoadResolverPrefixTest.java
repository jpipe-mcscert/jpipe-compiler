package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.OperatorCallConfig;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import ca.mcscert.jpipe.operators.builtin.RefineOperator;
import java.util.List;
import java.util.Map;
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
	void prefixesAQualifiedElementDeclarationLikeItsReferences() {
		// A model implementing t may declare t:local; its relations name it
		// t:local too, and both must become ns:t:local.
		var cmd = new CreateStrategy("j", "t:local", "S");
		var result = (CreateStrategy) prefixOne("ns", cmd);
		assertThat(result.container()).isEqualTo("ns:j");
		assertThat(result.identifier()).isEqualTo("ns:t:local");
	}

	@Test
	void prefixesTheQualifiedIdsOfARelation() {
		// t:s names an element inherited from model t, which the namespace
		// renames to ns:t (#178).
		var cmd = new AddSupport("j", "t:s", "e");
		var result = (AddSupport) prefixOne("ns", cmd);
		assertThat(result.supportableId()).isEqualTo("ns:t:s");
		assertThat(result.supporterId()).isEqualTo("e");
	}

	@Test
	void prefixesTheContainerAndTargetOfAnOverride() {
		// The override used to keep base:t:abs and miss ns:base:t:abs (#178).
		var cmd = new OverrideAbstractSupport("j", "base:t:abs", "evidence",
				"E");
		var result = (OverrideAbstractSupport) prefixOne("ns", cmd);
		assertThat(result.container()).isEqualTo("ns:j");
		assertThat(result.qualifiedId()).isEqualTo("ns:base:t:abs");
	}

	@Test
	void prefixesTheResultAndSourcesOfAnOperatorCall() {
		// An operator call was left as is: it waited for its unprefixed
		// sources forever (#178).
		OperatorRegistry operators = new OperatorRegistry();
		operators.register("refine", new RefineOperator());
		var cmd = new ApplyOperator(
				new OperatorCallConfig("r", "refine", List.of("a", "b"),
						Map.of("hook", "first:e", "unifyExclude", "a:x, y"),
						SourceLocation.UNKNOWN, ModelKind.JUSTIFICATION),
				operators, new UnificationEquivalenceRegistry());
		var result = (ApplyOperator) prefixOne("ns", cmd);
		assertThat(result.resultName()).isEqualTo("ns:r");
		assertThat(result.sourceNames()).containsExactly("ns:a", "ns:b");
		// A qualified id names an element of a model of the file: prefixed.
		assertThat(result.arguments()).containsEntry("hook", "ns:first:e")
				.containsEntry("unifyExclude", "ns:a:x,y");
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
