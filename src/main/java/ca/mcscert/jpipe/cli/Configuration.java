package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.error.ErrorManager;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration parameters obtained from command-line.
 *
 * @author mosser
 */
public final class Configuration {

    public static final String STDOUT_PATH = "<stdout>";
    public static final String STDIN_PATH  = "<stdin>";

    private static final Logger logger = LogManager.getLogger();

    private final String inputFilePath;
    private final String outputFilePath;

    private final String diagramName;
    private final Format format;

    private final Mode mode;


    /**
     * Automatically build a configuration from the command line parameters.
     * Any inconsistent configuration will stop the compilation process.
     *
     * @param parser the apache CLI parser
     */
    public Configuration(CommandLine parser) throws Exception {
        logger.info("Instantiating Configuration from command line");
        this.inputFilePath = readInput(parser);
        this.outputFilePath = readOutput(parser);
        this.diagramName = readName(parser);
        this.format = readFormat(parser);
        this.mode = readMode(parser);
        logger.trace(this);
        checkConsistency();
    }


    public String getDiagramName() {
        return diagramName;
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public Format getFormat() {
        return format;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Consistency rules (CR) for configuration.
     */
    private void checkConsistency()  {
        logger.trace("  Checking configuration consistency");
        // Nothing to do right now
    }

    /**
     * Read the mode the compiler will use.
     *
     * @param parser the apache CLI parser
     * @return specified mode (default is PRINT)
     */
    private Mode readMode(CommandLine parser) {
        logger.trace("  Identifying compiler mode");
        if (Option.COMPILE_MODE.isIn(parser)) {
            return Mode.valueOf(Option.COMPILE_MODE.readFrom(parser).toUpperCase());
        }
        return Mode.PRINT;
    }

    /**
     * File format to be used to export diagrams as pictures.
     *
     * @param parser the apache CLI parser
     * @return specified format (default is PNG)
     */
    private Format readFormat(CommandLine parser) {
        logger.trace("  Reading output format");
        if (Option.OUT_FORMAT.isIn(parser)) {
            return Format.valueOf(Option.OUT_FORMAT.readFrom(parser).toUpperCase());
        }
        return Format.PNG;
    }

    /**
     * Read the name of the diagram to process, if any.
     *
     * @param parser the apache CLI parser
     * @return the diagram name, null if non is provided
     */
    private String readName(CommandLine parser) {
        logger.trace("  Reading diagram name");
        if (Option.DIAG_NAME.isIn(parser)) {
            return Option.DIAG_NAME.readFrom(parser);
        }
        return null;
    }


    /**
     * Read the location of the output file for the compiler.
     *
     * @param parser the apache CLI parser
     * @return Filename to use for output.
     */
    private String readOutput(CommandLine parser) throws Exception {
        logger.trace("  Identifying output file/stream");
        return (Option.OUTPUT_PATH.isIn(parser)
                ? Option.OUTPUT_PATH.readFrom(parser)
                : STDOUT_PATH);
    }


    /**
     * Which input stream to use as input for the compiler.
     *
     * @param parser the apache CLI parser
     * @return stdin if no file provided, the provided file as stream elsewhere
     */
    private String readInput(CommandLine parser) throws Exception {
        logger.trace("  Identifying input file/stream");
        return (Option.INPUT_PATH.isIn(parser) ? Option.INPUT_PATH.readFrom(parser) : STDIN_PATH);
    }


    @Override
    public String toString() {
        return "Configuration{"
                + "inputFilePath='" + inputFilePath + '\''
                + ", outputFilePath='" + outputFilePath + '\''
                + ", diagramName='" + diagramName + '\''
                + ", format=" + format
                + ", mode=" + mode
                + '}';
    }
}
