package ca.mcscert.jpipe.cli;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Print message in color to the terminal using ANSI Code.
 */
public final class ColorPrinter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED   = "\u001B[31m";

    private final PrintStream stream;

    public ColorPrinter(OutputStream stream) {
        this.stream = new PrintStream(stream);
    }

    public void println(String message) {
        stream.println(ANSI_RED + message + ANSI_RESET);
    }

    public void print(String message) {
        stream.print(ANSI_RED + message + ANSI_RESET);
    }

}
