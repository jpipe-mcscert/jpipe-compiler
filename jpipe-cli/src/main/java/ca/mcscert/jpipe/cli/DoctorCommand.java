package ca.mcscert.jpipe.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/**
 * Checks that external tools required by jPipe are available on {@code PATH}.
 */
@Command(name = "doctor", description = "Check that required external tools are available on PATH.", mixinStandardHelpOptions = true)
class DoctorCommand implements Callable<Integer> {

	@Spec
	private CommandSpec spec;

	@ParentCommand
	private Main parent;

	@Override
	@SuppressWarnings("java:S106") // intentional: doctor output goes to stdout
	public Integer call() {
		if (!parent.headless) {
			Logo.sout();
		}
		System.out.println(spec.root().version()[0]);
		System.out.println("Checking external tools:");
		return Doctor.run() ? Main.EXIT_OK : Main.EXIT_JPIPE_ERROR;
	}
}
