package ca.mcscert.jpipe.cli;

import java.util.Set;
import picocli.CommandLine;
import picocli.CommandLine.Command;
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
		ProcessCommand.class}, mixinStandardHelpOptions = true, version = "jPipe 2.0.0")
public class Main {

	static final int EXIT_OK = 0;
	static final int EXIT_JPIPE_ERROR = 1;
	static final int EXIT_SYSTEM_ERROR = 42;

	private static final Set<String> SUBCOMMAND_NAMES = Set.of("doctor",
			"diagnostic", "process");
	private static final Set<String> HELP_FLAGS = Set.of("--help", "-h",
			"--version", "-V");
	private static final Set<String> PARENT_FLAGS = Set.of("--headless");

	@Option(names = {"--headless"}, description = "Suppress logo output.")
	boolean headless;

	/**
	 * Inserts {@code "process"} into {@code args} when no subcommand name is
	 * present, making {@code process} the effective default subcommand.
	 *
	 * <p>
	 * Help/version flags are left untouched so that {@code jpipe --help} still
	 * shows top-level help. The insertion point is placed after any
	 * parent-level flags ({@code --headless}) so that picocli can attribute
	 * them correctly.
	 */
	static String[] withDefaultSubcommand(String[] args) {
		for (String arg : args) {
			if (SUBCOMMAND_NAMES.contains(arg) || HELP_FLAGS.contains(arg)) {
				return args;
			}
		}
		int insertAt = 0;
		for (int i = 0; i < args.length; i++) {
			if (PARENT_FLAGS.contains(args[i])) {
				insertAt = i + 1;
			} else {
				break;
			}
		}
		String[] adjusted = new String[args.length + 1];
		System.arraycopy(args, 0, adjusted, 0, insertAt);
		adjusted[insertAt] = "process";
		System.arraycopy(args, insertAt, adjusted, insertAt + 1,
				args.length - insertAt);
		return adjusted;
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main())
				.setCaseInsensitiveEnumValuesAllowed(true)
				.execute(withDefaultSubcommand(args)));
	}
}
