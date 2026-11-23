package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The commands that bind an id the source wrote, through bind (#215). */
class BindingCallersTest {

	private Unit unit;
	private Justification j;

	@BeforeEach
	void setUp() {
		unit = new Unit("src");
		j = new Justification("j");
		j.addElement(new Strategy("s", "S"));
		j.addElement(new Evidence("a:e", "First"));
		j.addElement(new Evidence("b:e", "Second"));
		unit.add(j);
	}

	@Test
	void aRelationToAnAmbiguousIdIsRefused() {
		AddSupport support = new AddSupport("j", "s", "e");

		assertThatThrownBy(() -> support.execute(unit))
				.isInstanceOf(AmbiguousIdException.class)
				.hasMessageContainingAll("'a:e'", "'b:e'");
	}

	@Test
	void aRelationToAMissingIdIsRefused() {
		AddSupport support = new AddSupport("j", "s", "nowhere");

		assertThatThrownBy(() -> support.execute(unit))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("nowhere");
	}

	@Test
	void anOverrideOfNothingNamesNothing() {
		assertThat(new OverrideAbstractSupport("j", "nowhere", "evidence", "E")
				.namedIds(j)).isEmpty();
	}

	@Test
	void anOverrideOfAConcreteElementIsRefused() {
		OverrideAbstractSupport override = new OverrideAbstractSupport("j", "s",
				"evidence", "E");

		assertThatThrownBy(() -> override.expand(unit))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("No abstract support with id: s");
	}

	@Test
	void anOverrideOfAPlaceholderNamesIt() {
		j.addElement(new AbstractSupport("t:abs", "A"));

		assertThat(new OverrideAbstractSupport("j", "abs", "evidence", "E")
				.namedIds(j)).containsExactly("t:abs");
	}

	@Test
	void aCompositionIsRecordedWithTheScopesOfItsIdArguments() {
		unit.add(new Justification("out"));
		new RecordComposition("out", "assemble", List.of("j"), Map.of(),
				Map.of("unifyExclude", Composition.Scope.RESULT)).execute(unit);

		assertThat(unit.get("out").composition()).hasValueSatisfying(
				c -> assertThat(c.idArguments()).containsOnly(
						Map.entry("unifyExclude", Composition.Scope.RESULT)));
	}
}
