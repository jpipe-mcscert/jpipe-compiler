package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelectModelTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void named_selection_returns_matching_model() {
		Unit unit = new Unit("src");
		Justification j = new Justification("j");
		unit.add(j);

		JustificationModel<?> result = new SelectModel("j").fire(unit, ctx);

		assertThat(result).isSameAs(j);
	}

	@Test
	void named_selection_unknown_model_throws_compilation_exception() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j"));

		assertThatThrownBy(() -> new SelectModel("missing").fire(unit, ctx))
				.isInstanceOf(CompilationException.class);
	}

	@Test
	void auto_select_single_model_returns_it() {
		Unit unit = new Unit("src");
		Justification j = new Justification("j");
		unit.add(j);

		JustificationModel<?> result = new SelectModel(null).fire(unit, ctx);

		assertThat(result).isSameAs(j);
	}

	@Test
	void auto_select_multiple_models_throws_compilation_exception() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j1"));
		unit.add(new Justification("j2"));

		assertThatThrownBy(() -> new SelectModel(null).fire(unit, ctx))
				.isInstanceOf(CompilationException.class);
	}
}
