package ca.mcscert.jpipe.cli;

/** Prints the jPipe ASCII-art logo to standard output. */
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

	static void sout() {
		System.out.println(BANNER);
	}
}
