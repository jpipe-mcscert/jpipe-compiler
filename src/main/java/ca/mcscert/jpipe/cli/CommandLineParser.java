package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.Logo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class controls how the command line arguments are processed to configure the compiler.
 */
public final class CommandLineParser {

    private static final String APP_NAME = "java -jar jpipe.jar";
    private final String[] userGiven;

    /**
     * Constructor for the command line configuration.
     *
     * @param userGiven the argument string provided by the user when invoking the compiler
     */
    public CommandLineParser(String[] userGiven) {
        this.userGiven = userGiven;
    }

    /**
     * Parse the command line to extract configuration option asked by the user.
     *
     * @return Empty if the user asks for help (whatever the other options are),
     *         the parsed options elsewhere.
     * @throws ParseException if the arguments provided by the user does not match the CLI usage
     */
    public Optional<CommandLine> read() throws ParseException {
        org.apache.commons.cli.CommandLineParser parser = new DefaultParser();

        if (checkForHelp(userGiven)) {
            return Optional.empty();
        }

        Options options = getOptions();
        CommandLine cmd = parser.parse(options, userGiven);
        return Optional.of(cmd);
    }

    /**
     * Print the help message explaining to the user how to use the command line.
     */
    public void help() {
        Logo.sout();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(APP_NAME, getOptions(), true);
    }

    /**
     * Check in the provided arguments if the user invoked the "help" command (-h, --help).
     *
     * @param args the arguments provided by the user
     * @return true if the option is found
     */
    private boolean checkForHelp(String[] args) {
        Set<String> arguments = new HashSet<>(Arrays.asList(args));
        return (arguments.contains("-h") || arguments.contains("--help"));
    }

    /**
     * Configure the command line parser to know what to look for.
     *
     * @return an Options object used by Apache CLI to parse the provided arguments
     */
    private Options getOptions() {
        Options options = new Options();

        // always force the --help option
        org.apache.commons.cli.Option help = new org.apache.commons.cli.Option("h", "help", false,
                "display help message");
        options.addOption(help);

        Arrays.stream(Option.values()).forEach(o -> o.registerInto(options));
        return options;
    }
}

