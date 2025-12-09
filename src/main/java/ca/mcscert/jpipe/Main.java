package ca.mcscert.jpipe;

import ca.mcscert.jpipe.cli.ColorPrinter;
import ca.mcscert.jpipe.cli.CommandLineParser;
import ca.mcscert.jpipe.cli.Configuration;
import ca.mcscert.jpipe.cli.Option;
import ca.mcscert.jpipe.compiler.Compiler;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.error.ErrorManager;
import ca.mcscert.jpipe.error.FatalException;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Entry point for the compiler.
 *
 * @author mosser
 */
public final class Main {

    private static final Logger logger = LogManager.getLogger();

    private static final int SUCCESS_CODE = 0;
    private static final int ERROR_CODE = 1;
    private static final int UNEXPECTED_CODE = 42;

    /**
     * Entry point of the compiler.
     *
     * @param args arguments provided on the command line by the user
     */
    public static void main(String[] args) {
        CommandLineParser cli = null;
        try {
            cli = new CommandLineParser(args);
            Optional<CommandLine> arguments = cli.read();
            if (arguments.isEmpty()) {
                cli.help();
                System.exit(SUCCESS_CODE);
            }
            setupLogLevel(arguments.get());
            compile(arguments.get());
            flush();
        } catch (FatalException fe) {
            flush(ERROR_CODE);
        } catch (UnrecognizedOptionException ure) {
            cli.help();
            ColorPrinter printer = new ColorPrinter(System.err);
            printer.println("\n" + ure.getMessage());
            System.exit(UNEXPECTED_CODE);
        } catch (Exception e) {
            ErrorManager.getInstance().registerError(e);
            flush(UNEXPECTED_CODE);
        }
    }

    private static void compile(CommandLine arguments) throws Exception {
        logger.info("Starting jPipe compiler");
        Configuration config = new Configuration(arguments);
        Compiler compiler = CompilerFactory.build(config);
        compiler.compile(config.getInputFilePath(), config.getOutputFilePath());
        logger.info("End of compilation process");
    }

    /**
     * Reconfigure Log4J2 to use the provided log level. Default is OFF (if none provided)
     *
     * @param args raw arguments from the command line.
     */
    private static void setupLogLevel(CommandLine args) {
        Level lvl = (Option.LOG_LVL.isIn(args)
                        ? Level.getLevel(Option.LOG_LVL.readFrom(args))
                        : Level.OFF);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration c = ctx.getConfiguration();
        LoggerConfig loggerConfig = c.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(lvl);
        ctx.updateLoggers();
    }

    /**
     * Flush the error manager and use the provided exit code.
     *
     * @param exitCode the system code to use to tell the shell the execution status
     */
    private static void flush(int exitCode) {
        System.err.print(ErrorManager.getInstance().toString());
        if (exitCode != SUCCESS_CODE) {
            ColorPrinter printer = new ColorPrinter(System.err);
            printer.println("Compilation process ended with errors");
            printer.println("  -->> Exit code: " + exitCode + " <<--");
        }
        System.exit(exitCode);
    }

    private static void flush() {
        if (ErrorManager.getInstance().length() == 0) {
            System.exit(SUCCESS_CODE);
        } else {
            flush(ERROR_CODE);
        }
    }


}
