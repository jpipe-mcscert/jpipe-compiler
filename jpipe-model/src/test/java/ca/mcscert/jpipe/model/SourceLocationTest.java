package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceLocationTest {

	@Test
	void unknownSentinelIsNotKnown() {
		assertThat(SourceLocation.UNKNOWN.isKnown()).isFalse();
	}

	@Test
	void knownLocationIsKnown() {
		assertThat(new SourceLocation(3, 5).isKnown()).isTrue();
	}

	@Test
	void toStringForKnownLocation() {
		assertThat(new SourceLocation(3, 5).toString()).isEqualTo("3:5");
	}

	@Test
	void toStringForUnknown() {
		assertThat(SourceLocation.UNKNOWN.toString()).isEqualTo("<unknown>");
	}

	@Test
	void recordEqualityByValue() {
		assertThat(new SourceLocation(3, 5))
				.isEqualTo(new SourceLocation(3, 5));
	}

	@Test
	void recordInequalityOnDifferentValues() {
		assertThat(new SourceLocation(3, 5))
				.isNotEqualTo(new SourceLocation(3, 6));
	}

	@Test
	void toStringIncludesSourceWhenPresent() {
		assertThat(new SourceLocation("foo.jd", 3, 5).toString())
				.isEqualTo("foo.jd:3:5");
	}

	@Test
	void toStringWithoutSourceOmitsFilename() {
		assertThat(new SourceLocation(3, 5).toString()).isEqualTo("3:5");
	}

	@Test
	void locationWithSourceIsKnown() {
		assertThat(new SourceLocation("foo.jd", 3, 5).isKnown()).isTrue();
	}

	@Test
	void equalityConsidersSource() {
		assertThat(new SourceLocation("a.jd", 3, 5))
				.isNotEqualTo(new SourceLocation("b.jd", 3, 5));
	}
}
