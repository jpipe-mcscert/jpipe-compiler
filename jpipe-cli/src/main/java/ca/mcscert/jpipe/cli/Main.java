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
			"--log-level"}, description = "Log verbosity: OFF, ERROR, WARN, INFO, DEBUG, TRACE (default: ERROR).")
	void setLogLevel(String level) {
		Configurator.setLevel("ca.mcscert.jpipe",
				Level.toLevel(level, Level.ERROR));
	}

	/**
	 * Inserts {@code "process"} into {@code args} when no subcommand name is
	 * present, making {@code process} the effective default subcommand.
	 *
	 * <p>
	 * Picocli 4.x has no built-in default-subcommand API, so we pre-process the
	 * argument list. Subcommand names, help, and version flags are derived from
	 * {@code cmd} at call time, so this method stays correct when subcommands
	 * or parent options are added. The insertion point is placed after any
	 * parent-level flags so that picocli attributes them to the parent command
	 * correctly.
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

		for (String arg : args) {
			if (shortCircuit.contains(arg)) {
				return args;
			}
		}

		int insertAt = 0;
		int i = 0;
		while (i < args.length && parentFlags.containsKey(args[i])) {
			i += 1 + parentFlags.get(args[i]);
			insertAt = i;
		}

		String[] adjusted = new String[args.length + 1];
		System.arraycopy(args, 0, adjusted, 0, insertAt);
		adjusted[insertAt] = "process";
		System.arraycopy(args, insertAt, adjusted, insertAt + 1,
				args.length - insertAt);
		return adjusted;
	}

	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new Main())
				.setCaseInsensitiveEnumValuesAllowed(true);
		System.exit(cmd.execute(withDefaultSubcommand(args, cmd)));
	}
}
