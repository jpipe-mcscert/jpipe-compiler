package ca.mcscert.jpipe.cli;

/**
 * The jPipe ASCII-art logo.
 *
 * <p>
 * The commands print it on standard error: it is a greeting for a human, never
 * part of what a command produces. On standard output it would sit in front of
 * every export and report written there, and corrupt the JSON, Python or image
 * a caller pipes elsewhere (#175).
 *
 * <p>
 * The callers print it themselves, from methods whose console output is
 * intentional by design.
 */
final class Logo {

	static final String BANNER = """
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
}
