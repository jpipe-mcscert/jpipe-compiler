package ca.mcscert.jpipe;

/**
 * Pretty stupid utility class that prints the JPipe logo.
 *
 * @author mosser
 */
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

    /**
     * print out the logo on stdout.
     */
    public static void sout() {
        System.out.println(new Logo());
    }

    /**
     * print out the logo on stderr (never used so far).
     */
    public static void serr() {
        System.err.println(new Logo());
    }



}
