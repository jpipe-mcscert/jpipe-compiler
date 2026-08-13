package ca.mcscert.jpipe.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that external tools required by jPipe are available on {@code PATH}
 * and reports their status to standard output.
 *
 * <p>
 * Each tool is probed by attempting to start a process. A tool is considered
 * available if the OS can locate and launch the executable, regardless of its
 * exit code. When the probe output advertises a version number, it is reported
 * alongside the status: some operating systems ship outdated versions, which is
 * a common source of rendering issues.
 */
final class Doctor {

	/** Maps a human-readable tool name to the command used to probe it. */
	private static final Map<String, String[]> TOOLS = new LinkedHashMap<>();

	/** Extracts a version number out of a {@code --version} style banner. */
	private static final Pattern VERSION = Pattern
			.compile("version\\s+v?(\\d[\\w.+-]*)", Pattern.CASE_INSENSITIVE);

	static {
		TOOLS.put("dot (Graphviz)", new String[]{"dot", "-V"});
	}

	private Doctor() {
	}

	/**
	 * Probes all required external tools and prints a status line for each.
	 *
	 * @return {@code true} if every tool is available, {@code false} if any is
	 *         missing.
	 */
	static boolean run() {
		boolean allOk = true;
		for (Map.Entry<String, String[]> entry : TOOLS.entrySet()) {
			String name = entry.getKey();
			Optional<String> banner = probe(entry.getValue());
			if (banner.isPresent()) {
				System.out.println(
						"  " + name + ": OK (" + describe(banner.get()) + ")");
			} else {
				System.out.println("  " + name + ": NOT FOUND");
				allOk = false;
			}
		}
		return allOk;
	}

	/**
	 * Runs a probe command and captures what it printed.
	 *
	 * @param command
	 *            the command to run.
	 * @return the (possibly empty) probe output, or {@link Optional#empty()} if
	 *         the executable could not be launched.
	 */
	private static Optional<String> probe(String[] command) {
		try {
			Process p = new ProcessBuilder(command).redirectErrorStream(true)
					.start();
			String output;
			try (InputStream in = p.getInputStream()) {
				output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			p.waitFor();
			return Optional.of(output);
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (IOException _) {
			return Optional.empty();
		}
	}

	/**
	 * Turns a probe banner into a human-readable version description.
	 *
	 * @param banner
	 *            the raw output captured from the probe command.
	 * @return {@code "version X.Y.Z"}, or {@code "version unknown"} when the
	 *         banner does not advertise one.
	 */
	static String describe(String banner) {
		Matcher matcher = VERSION.matcher(banner);
		if (matcher.find()) {
			return "version " + matcher.group(1);
		}
		return "version unknown";
	}
}
