package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.model.SourceLocation;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicateModelFilterTest {

	private final CompilationContext ctx = new CompilationContext("main.jd");

	@Test
	void distinct_models_are_kept_as_they_are() {
		List<Command> commands = List.of(new CreateJustification("a"),
				new CreateConclusion("a", "c", "C"), new CreateTemplate("t"));

		assertThat(new DuplicateModelFilter().fire(commands, ctx))
				.isEqualTo(commands);
		assertThat(ctx.diagnostics()).isEmpty();
	}

	@Test
	void a_second_declaration_is_reported_and_dropped_with_its_body() {
		// Its body used to run against the first model, changing it (#183).
		Command first = new CreateTemplate("t",
				new SourceLocation("lib.jd", 8, 9));
		Command firstBody = new CreateConclusion("t", "c", "C");
		Command again = new CreateTemplate("t",
				new SourceLocation("main.jd", 12, 9));
		Command againBody = new CreateConclusion("t", "c2", "C2");
		Command next = new CreateJustification("j");
		Command nextBody = new CreateConclusion("j", "c", "C");

		List<Command> kept = new DuplicateModelFilter().fire(
				List.of(first, firstBody, again, againBody, next, nextBody),
				ctx);

		assertThat(kept).containsExactly(first, firstBody, next, nextBody);
		assertThat(ctx.diagnostics()).singleElement().satisfies(d -> {
			assertThat(d.code()).isEqualTo("duplicate-model");
			assertThat(d.source()).isEqualTo("main.jd");
			assertThat(d.line()).isEqualTo(12);
			assertThat(d.message())
					.isEqualTo("Model 't' is already declared at lib.jd:8");
		});
	}

	@Test
	void a_justification_and_a_template_cannot_share_a_name() {
		List<Command> kept = new DuplicateModelFilter().fire(
				List.of(new CreateJustification("m"), new CreateTemplate("m")),
				ctx);

		assertThat(kept).hasSize(1);
		assertThat(ctx.diagnostics()).extracting(Diagnostic::code)
				.containsExactly("duplicate-model");
	}
}
