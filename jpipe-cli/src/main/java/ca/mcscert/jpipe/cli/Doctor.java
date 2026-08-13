package ca.mcscert.jpipe.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that external tools required by jPipe are available on {@code PATH}
 * and reports their status to a given stream.
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
	 * Probes all required external tools and reports a status line for each.
	 *
	 * @param out
	 *            where the status lines are written.
	 * @return {@code true} if every tool is available, {@code false} if any is
	 *         missing.
	 */
	static boolean run(PrintStream out) {
		return run(TOOLS, out);
	}

	/**
	 * Probes the given tools and reports a status line for each.
	 *
	 * @param tools
	 *            the tools to probe, mapping a name to its probe command.
	 * @param out
	 *            where the status lines are written.
	 * @return {@code true} if every tool is available, {@code false} if any is
	 *         missing.
	 */
	static boolean run(Map<String, String[]> tools, PrintStream out) {
		boolean allOk = true;
		for (Map.Entry<String, String[]> entry : tools.entrySet()) {
			Optional<String> banner = probe(entry.getValue());
			out.println(statusLine(entry.getKey(), banner));
			allOk = allOk && banner.isPresent();
		}
		return allOk;
	}

	/**
	 * Runs a probe command and captures what it printed.
	 *
	 * <p>
	 * The probe output is read to end-of-file, which the tool reaches when it
	 * terminates; its exit code is deliberately ignored. A tool that starts but
	 * whose output cannot be read is still reported as available, with an empty
	 * banner.
	 *
	 * @param command
	 *            the command to run.
	 * @return the (possibly empty) probe output, or {@link Optional#empty()} if
	 *         the executable could not be launched.
	 */
	static Optional<String> probe(String[] command) {
		Process process;
		try {
			process = new ProcessBuilder(command).redirectErrorStream(true)
					.start();
		} catch (IOException _) {
			return Optional.empty();
		}
		try (InputStream in = process.getInputStream()) {
			return Optional
					.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException _) {
			process.destroy();
			return Optional.of("");
		}
	}

	/**
	 * Builds the status line reported for a tool.
	 *
	 * @param name
	 *            the human-readable tool name.
	 * @param banner
	 *            the probe output, empty if the tool could not be launched.
	 * @return the line to print for that tool.
	 */
	static String statusLine(String name, Optional<String> banner) {
		String status = banner.map(b -> "OK (" + describe(b) + ")")
				.orElse("NOT FOUND");
		return "  " + name + ": " + status;
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
