package ca.mcscert.jpipe.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Options available from the command line to configure the compiler.
 *
 * @author mosser
 */
public enum Option {

    INPUT_PATH("i", "input",
            "input file path (default: stdin)"),

    OUTPUT_PATH("o", "output",
            "output file path (default: stdout)"),

    DIAG_NAME("d", "diagram",
            "diagram name (optional if only one)"),

    OUT_FORMAT("f", "format",
            "output format in [png, svg, dot, json, jpipe, runner]\n(Default: png)"),

    COMPILE_MODE("mode",
            "compiler's mode in [print, list] (default: print)"),

    LOG_LVL("log-level",
            "log level for Java logging API\n(default: OFF)")
    ;

    private final String shortName;
    private final String longName;
    private final String description;
    private final boolean hasArgs;

    /**
     * Instantiate an Option with minimal information.
     *
     * @param longName the long option mnemonic ('--something_long')
     * @param description semantics of the option
     */
    Option(String longName, String description) {
        this(null, longName, description, true);
    }

    /**
     * Instantiate an option, including shortname.
     *
     * @param shortName the short option mnemonic ('-s')
     * @param longName the long option mnemonic ('--something_long')
     * @param description semantics of the option
     */
    Option(String shortName, String longName, String description) {
        this(shortName, longName, description, true);
    }


    /**
     * Instantiate an option (with all needed info).
     *
     * @param shortName the short option mnemonic ('-s')
     * @param longName the long option mnemonic ('--something_long')
     * @param description semantics of the option
     * @param hasArgs does the option requires an argument?
     */
    Option(String shortName, String longName, String description, boolean hasArgs) {
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
        this.hasArgs = hasArgs;
    }

    /**
     * Register the current Option into an Apache CLI set of Options.
     *
     * @param opts the CLI option set
     */
    public void registerInto(Options opts) {
        org.apache.commons.cli.Option opt = new org.apache.commons.cli.Option(
                this.shortName, this.longName, this.hasArgs, this.description
        );
        opts.addOption(opt);
    }

    /**
     * Check if this option is set in the provided CommandLine.
     *
     * @param cli the command line to check.
     * @return true if the option is set, false elsewhere.
     */
    public boolean isIn(CommandLine cli) {
        return cli.hasOption(this.longName);
    }

    /**
     * Read the value of the option provided in the CommandLine.
     *
     * @param cli the command line to check.
     * @return the provided value (assumes there is one)
     */
    public String readFrom(CommandLine cli) {
        return cli.getOptionValue(this.longName);
    }

}
