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
