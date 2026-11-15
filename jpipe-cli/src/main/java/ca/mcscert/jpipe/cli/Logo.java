package ca.mcscert.jpipe.cli;

/**
 * Prints the jPipe ASCII-art logo to standard error.
 *
 * <p>
 * The banner is a greeting for a human, never part of what a command produces:
 * on standard output it would sit in front of every export and report written
 * there, and corrupt the JSON, Python or image a caller pipes elsewhere (#175).
 */
final class Logo {

	private static final String BANNER = """
			McMaster University - McSCert (c) 2023-...
			    _   ___ _
			   (_) / _ (_)_ __   ___
			   | |/ /_)/ | '_ \\ / _ \\
			   | / ___/| | |_) |  __/
			  _/ \\/    |_| .__/ \\___|
			 |__/        |_|
			""";

	private Logo() {
	}

	static void print() {
		System.err.println(BANNER);
	}
}
