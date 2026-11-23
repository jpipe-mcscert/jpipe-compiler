package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.SourceLocation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reports every model declared under a name already taken, and drops that
 * declaration, before any command runs.
 *
 * <p>
 * The commands of one model declaration are contiguous in the list: its
 * creation (a {@code justification}, a {@code template} or an operator call)
 * comes first, its body follows, up to the next creation. That holds across
 * {@code load}-ed files, whose commands {@link LoadResolver} splices in whole.
 * So a second declaration of a name is dropped with everything after it up to
 * the next creation, and nothing it contains lands in the first model.
 *
 * <p>
 * Left to the execution engine, the second creation would fail, while the body
 * that follows it would run against the first model and change it without a
 * word (#183).
 */
public final class DuplicateModelFilter
		extends
			Transformation<List<Command>, List<Command>> {

	@Override
	protected List<Command> run(List<Command> commands,
			CompilationContext ctx) {
		Map<String, SourceLocation> declared = new HashMap<>();
		List<Command> kept = new ArrayList<>(commands.size());
		boolean dropping = false;
		for (Command cmd : commands) {
			Optional<String> name = cmd.declares();
			if (name.isPresent()) {
				SourceLocation first = declared.putIfAbsent(name.get(),
						cmd.location());
				dropping = first != null;
				if (dropping) {
					ctx.error(DiagnosticCodes.DUPLICATE_MODEL, cmd.location(),
							"Model '" + name.get() + "' is already declared"
									+ where(first));
				}
			}
			if (!dropping) {
				kept.add(cmd);
			}
		}
		return kept;
	}

	/** {@code " at lib.jd:8"}, or nothing when the first one is unlocated. */
	private static String where(SourceLocation first) {
		if (!first.isKnown()) {
			return "";
		}
		String file = first.source() != null
				? fileName(first.source()) + ":"
				: "line ";
		return " at " + file + first.line();
	}

	/**
	 * The file name of {@code source}, or {@code <stdin>} as is (see
	 * {@link CompilationConfig#isStdin}).
	 */
	private static String fileName(String source) {
		return CompilationConfig.isStdin(source)
				? source
				: String.valueOf(Path.of(source).getFileName());
	}
}
