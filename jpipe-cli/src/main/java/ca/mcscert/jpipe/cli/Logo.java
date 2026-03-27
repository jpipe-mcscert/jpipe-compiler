package ca.mcscert.jpipe.cli;

/** Prints the jPipe ASCII-art logo to standard output or standard error. */
public final class Logo {

	@Override
	public String toString() {
		return """
				McMaster University - McSCert (c) 2023-...
				    _   ___ _
				   (_) / _ (_)_ __   ___
				   | |/ /_)/ | '_ \\ / _ \\
				   | / ___/| | |_) |  __/
				  _/ \\/    |_| .__/ \\___|
				 |__/        |_|
				""";
	}

	public static void sout() {
		System.out.println(new Logo());
	}

	public static void serr() {
		System.err.println(new Logo());
	}
}
