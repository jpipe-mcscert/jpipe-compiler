package ca.mcscert.jpipe.cli;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;

/**
 * jPipe CLI entry point.
 *
 * <p>
 * Three subcommands are available: {@code doctor}, {@code diagnostic}, and
 * {@code process}. When no subcommand is specified {@code process} is used as
 * the default.
 */
@Command(name = "jpipe", description = "Compile and process jPipe justification files.", subcommands = {
		DoctorCommand.class, DiagnosticCommand.class,
		ProcessCommand.class}, mixinStandardHelpOptions = true, versionProvider = Main.ManifestVersionProvider.class)
public class Main {

	static class ManifestVersionProvider
			implements
				CommandLine.IVersionProvider {
		@Override
		public String[] getVersion() {
			String v = Main.class.getPackage().getImplementationVersion();
			return new String[]{"jPipe " + (v != null ? v : "dev")};
		}
	}

	static final int EXIT_OK = 0;
	static final int EXIT_JPIPE_ERROR = 1;
	static final int EXIT_SYSTEM_ERROR = 42;

	@Option(names = {"--headless"}, description = "Suppress logo output.")
	boolean headless;

	@Option(names = {
			"--log-level"}, paramLabel = "LEVEL", description = "Log verbosity: ${COMPLETION-CANDIDATES} (default: ERROR). Compilation errors are always shown.")
	void setLogLevel(LogLevel level) {
		// Under the simple back end, LoggingBootstrap already set the level,
		// and log4j-core has no configuration to change (#216).
		if (!LoggingBootstrap.usesSimpleLogger(System.getProperties())) {
			Configurator.setLevel("ca.mcscert.jpipe", level.toLog4j());
		}
	}

	/**
	 * The levels {@code --log-level} accepts. Declared as an enum so that
	 * picocli rejects anything else, as it does for {@code -f}, instead of
	 * silently falling back to {@code ERROR} (#186).
	 */
	enum LogLevel {
		OFF, ERROR, WARN, INFO, DEBUG, TRACE;

		Level toLog4j() {
			return Level.valueOf(name());
		}
	}

	/**
	 * Inserts {@code "process"} into {@code args} when no subcommand is given,
	 * making {@code process} the effective default subcommand.
	 *
	 * <p>
	 * Picocli 4.x has no built-in default-subcommand API, so we pre-process the
	 * argument list. Parent-level flags come first, in either the
	 * {@code --log-level INFO} or the {@code --log-level=INFO} form. The token
	 * right after them decides: a subcommand name, or the help or version flag,
	 * leaves the arguments as they are; anything else gets {@code process}
	 * inserted in front of it. Only that position counts, so a model or a file
	 * named like a subcommand further down the line is just an argument (#186).
	 *
	 * <p>
	 * Subcommand names, help and version flags and the arity of each parent
	 * flag are derived from {@code cmd} at call time, so this method stays
	 * correct when subcommands or parent options are added. A parent flag
	 * missing its value is left for picocli to report.
	 */
	static String[] withDefaultSubcommand(String[] args, CommandLine cmd) {
		Set<String> shortCircuit = new HashSet<>(cmd.getSubcommands().keySet());
		Map<String, Integer> parentFlags = new HashMap<>();
		for (OptionSpec opt : cmd.getCommandSpec().options()) {
			if (opt.usageHelp() || opt.versionHelp()) {
				Collections.addAll(shortCircuit, opt.names());
			} else {
				int arity = opt.arity().max();
				for (String name : opt.names()) {
					parentFlags.put(name, arity);
				}
			}
		}

		int insertAt = 0;
		int length = parentFlagLength(args, insertAt, parentFlags);
		while (length > 0) {
			insertAt += length;
			length = parentFlagLength(args, insertAt, parentFlags);
		}
		if (insertAt > args.length || (insertAt < args.length
				&& (shortCircuit.contains(args[insertAt])
						|| isUnknownOption(args[insertAt],
								cmd.getSubcommands().get("process"))))) {
			return args;
		}

		String[] adjusted = new String[args.length + 1];
		System.arraycopy(args, 0, adjusted, 0, insertAt);
		adjusted[insertAt] = "process";
		System.arraycopy(args, insertAt, adjusted, insertAt + 1,
				args.length - insertAt);
		return adjusted;
	}

	/**
	 * Whether {@code token} looks like an option but is none of
	 * {@code process}'s: most likely a mistyped parent flag, as in
	 * {@code jpipe --headles doctor}. Left in place, picocli reports it against
	 * the root command, rather than {@code process} complaining about it and
	 * about the subcommand that follows. Accepts {@code --name=value} and the
	 * attached short form {@code -ifile.jd}.
	 */
	private static boolean isUnknownOption(String token, CommandLine process) {
		if (!token.startsWith("-") || token.equals("-")) {
			return false;
		}
		Set<String> names = new HashSet<>();
		for (OptionSpec opt : process.getCommandSpec().options()) {
			Collections.addAll(names, opt.names());
		}
		int equals = token.indexOf('=');
		String name = equals > 0 ? token.substring(0, equals) : token;
		boolean attachedShort = !token.startsWith("--") && token.length() > 2
				&& names.contains(token.substring(0, 2));
		return !names.contains(name) && !attachedShort;
	}

	/**
	 * How many tokens the parent flag at {@code args[at]} spans: one for
	 * {@code --flag=value}, one plus its arity for {@code --flag value}, and
	 * {@code 0} when that token is not a parent flag or is past the end.
	 */
	private static int parentFlagLength(String[] args, int at,
			Map<String, Integer> parentFlags) {
		if (at >= args.length) {
			return 0;
		}
		String arg = args[at];
		Integer arity = parentFlags.get(arg);
		if (arity != null) {
			return 1 + arity;
		}
		int equals = arg.indexOf('=');
		return equals > 0 && parentFlags.containsKey(arg.substring(0, equals))
				? 1
				: 0;
	}

	/**
	 * The configured CLI parser.
	 *
	 * <p>
	 * Both {@link #main(String[])} and the acceptance tests go through here, so
	 * that tests exercise the same parser configuration users get — notably
	 * case-insensitive enum values, which let {@code -f json} work as well as
	 * {@code -f JSON}, and the handler that turns an unexpected exception into
	 * exit code 42.
	 *
	 * @return a {@link CommandLine} bound to a fresh {@link Main}.
	 */
	static CommandLine commandLine() {
		return new CommandLine(new Main())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.setExecutionExceptionHandler(new UnexpectedErrorHandler());
	}

	public static void main(String[] args) {
		// Before anything asks for a logger: see LoggingBootstrap.
		LoggingBootstrap.select(args, System.getProperties());
		CommandLine cmd = commandLine();
		System.exit(cmd.execute(withDefaultSubcommand(args, cmd)));
	}
}
