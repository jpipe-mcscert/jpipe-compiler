package ca.mcscert.jpipe.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks that external tools required by jPipe are available on {@code PATH}
 * and reports their status to standard output.
 *
 * <p>
 * Each tool is probed by attempting to start a process. A tool is considered
 * available if the OS can locate and launch the executable, regardless of its
 * exit code.
 */
final class Doctor {

	/** Maps a human-readable tool name to the command used to probe it. */
	private static final Map<String, String[]> TOOLS = new LinkedHashMap<>();

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
			String[] command = entry.getValue();
			if (isAvailable(command)) {
				System.out.println("  " + name + ": OK");
			} else {
				System.out.println("  " + name + ": NOT FOUND");
				allOk = false;
			}
		}
		return allOk;
	}

	private static boolean isAvailable(String[] command) {
		try {
			Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
			p.getInputStream().transferTo(OutputStream.nullOutputStream());
			p.waitFor();
			return true;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}
}
