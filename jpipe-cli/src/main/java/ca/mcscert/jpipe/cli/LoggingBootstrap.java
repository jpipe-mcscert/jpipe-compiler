package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.cli.Main.LogLevel;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Chooses the Log4j back end before anything logs (#216).
 *
 * <p>
 * Starting log4j-core and reading its configuration is a large share of a run's
 * start-up time, and a run at the default level has nothing to log: the
 * diagnostics reach standard error without going through a logger, and the
 * compiler logs nothing at {@code ERROR}. So unless {@code --log-level} asks
 * for {@code WARN} or more, the run uses the simple logger built into the Log4j
 * API instead, at the requested level. Asking for more detail starts log4j-core
 * and its configuration, as before.
 *
 * <p>
 * Log4j settles on its back end the first time a class asks for a logger, which
 * happens while picocli builds the commands, before it parses
 * {@code --log-level}. That is why the arguments are read here, ahead of
 * picocli, and why {@link Main#main} calls this first.
 */
final class LoggingBootstrap {

	/** The Log4j property naming the back end to use. */
	static final String PROVIDER = "log4j.provider";

	/** The simple logger built into the Log4j API. */
	static final String SIMPLE_PROVIDER = "org.apache.logging.log4j.simple.internal.SimpleProvider";

	private static final String SIMPLE_LOG = "org.apache.logging.log4j.simplelog.";

	private static final String LOG_LEVEL = "--log-level";

	/** How picocli marks an argument file, whose content it expands. */
	private static final String ARG_FILE = "@";

	private LoggingBootstrap() {
	}

	/**
	 * Selects the simple back end in {@code system} when the run asks for no
	 * more than {@code ERROR}.
	 *
	 * <p>
	 * Log4j starts as it always did, with log4j-core, when this cannot be sure
	 * of either: when the user configured Log4j, through any {@code log4j}
	 * system property or {@code LOG4J_} environment variable (a provider, a
	 * configuration file), and when an {@code @file} argument may hold
	 * {@code --log-level}, which picocli expands only after Log4j has started.
	 *
	 * @param args
	 *            the command line, as {@link Main#main} receives it.
	 * @param system
	 *            the system properties Log4j reads at start-up.
	 * @param env
	 *            the environment Log4j reads at start-up.
	 */
	static void select(String[] args, Properties system,
			Map<String, String> env) {
		if (userConfiguredLog4j(system, env) || Arrays.stream(args)
				.anyMatch(arg -> arg.startsWith(ARG_FILE))) {
			return;
		}
		LogLevel level = requestedLevel(args);
		if (needsCore(level)) {
			return;
		}
		system.setProperty(PROVIDER, SIMPLE_PROVIDER);
		system.setProperty(SIMPLE_LOG + "level", level.name());
		system.setProperty(SIMPLE_LOG + "showdatetime", "true");
		system.setProperty(SIMPLE_LOG + "dateTimeFormat", "HH:mm:ss.SSS");
	}

	private static boolean userConfiguredLog4j(Properties system,
			Map<String, String> env) {
		return system.stringPropertyNames().stream().anyMatch(
				key -> key.toLowerCase(Locale.ROOT).startsWith("log4j"))
				|| env.keySet().stream().anyMatch(key -> key
						.toUpperCase(Locale.ROOT).startsWith("LOG4J_"));
	}

	/**
	 * Whether {@link #select} chose the simple logger in {@code system}. Read
	 * from the property rather than from Log4j itself, which would load
	 * log4j-core's classes to answer.
	 */
	static boolean usesSimpleLogger(Properties system) {
		return SIMPLE_PROVIDER.equals(system.getProperty(PROVIDER));
	}

	/** Whether {@code level} needs log4j-core and its configuration. */
	static boolean needsCore(LogLevel level) {
		return level.compareTo(LogLevel.ERROR) > 0;
	}

	/**
	 * The level {@code --log-level} asks for, read where picocli reads it:
	 * among the parent flags, before the subcommand or the first argument.
	 * {@code ERROR}, the default, when it is absent, and when its value is not
	 * a level, which picocli then reports.
	 */
	static LogLevel requestedLevel(String[] args) {
		int i = 0;
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i];
			if (arg.equals(LOG_LEVEL)) {
				return i + 1 < args.length
						? parse(args[i + 1])
						: LogLevel.ERROR;
			}
			if (arg.startsWith(LOG_LEVEL + "=")) {
				return parse(arg.substring(LOG_LEVEL.length() + 1));
			}
			i++;
		}
		return LogLevel.ERROR;
	}

	private static LogLevel parse(String value) {
		try {
			return LogLevel.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException _) {
			return LogLevel.ERROR;
		}
	}
}
