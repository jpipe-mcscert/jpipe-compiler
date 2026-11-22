package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddElement;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.MarkUnified;
import ca.mcscert.jpipe.commands.linking.RecordComposition;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.commands.linking.RemoveElement;
import ca.mcscert.jpipe.commands.linking.RewireStrategySupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CommandNamespaceTest {

	static Stream<Command> builtWhileTheUnitIs() {
		return Stream.of(new RegisterAlias("m", "a:s", "s"),
				new MarkUnified("m", "unified_0"),
				new RecordComposition(
						"m", "assemble", List.of("a"), Map.of(), Map.of()),
				new RemoveElement("m", "e"),
				new RewireStrategySupport("m", "s", "e", "Evidence", "e",
						"SubConclusion"),
				new AddElement("m", new Evidence("e", "E")));
	}

	@ParameterizedTest
	@MethodSource("builtWhileTheUnitIs")
	void aCommandNeverReadFromAFileRefusesANamespace(Command command) {
		assertThatThrownBy(() -> command.withNamespace("ns"))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("never read from a file");
	}

	static Stream<Arguments> readFromAFile() {
		return Stream.of(
				Arguments.of(new CreateJustification("j"),
						new CreateJustification("ns:j")),
				Arguments.of(new CreateTemplate("t"),
						new CreateTemplate("ns:t")),
				Arguments.of(new CreateEvidence("j", "e", "E"),
						new CreateEvidence("ns:j", "e", "E")),
				Arguments.of(new CreateEvidence("j", "t:e", "E"),
						new CreateEvidence("ns:j", "ns:t:e", "E")),
				Arguments.of(new ImplementsTemplate("j", "t"),
						new ImplementsTemplate("ns:j", "ns:t")),
				Arguments.of(new AddSupport("j", "s", "t:e"),
						new AddSupport("ns:j", "s", "ns:t:e")),
				Arguments.of(
						new OverrideAbstractSupport("j", "t:abs", "evidence",
								"E"),
						new OverrideAbstractSupport("ns:j", "ns:t:abs",
								"evidence", "E")));
	}

	@ParameterizedTest
	@MethodSource("readFromAFile")
	void aCommandReadFromAFileQualifiesModelsAndQualifiedIds(Command command,
			Command namespaced) {
		assertThat(command.withNamespace("ns"))
				.hasToString(namespaced.toString());
	}

	static Stream<Command> declaringNothing() {
		return Stream.of(new CreateEvidence("m", "e", "E"),
				new AddSupport("m", "s", "e"), new RemoveElement("m", "e"));
	}

	@ParameterizedTest
	@MethodSource("declaringNothing")
	void aCommandInsideADeclarationDeclaresNoModel(Command command) {
		assertThat(command.declares()).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("declaringAModel")
	void aModelCreationDeclaresItsModel(Command command) {
		assertThat(command.declares()).contains("m");
	}

	static Stream<Command> declaringAModel() {
		return Stream.of(new CreateJustification("m"), new CreateTemplate("m"));
	}
}
