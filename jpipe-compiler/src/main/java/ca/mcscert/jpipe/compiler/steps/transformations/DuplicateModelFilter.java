package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.operators.ApplyOperator;
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
			Optional<String> name = declaredModel(cmd);
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

	/** The model {@code cmd} declares, if it starts a model declaration. */
	private static Optional<String> declaredModel(Command cmd) {
		return switch (cmd) {
			case CreateJustification c -> Optional.of(c.identifier());
			case CreateTemplate c -> Optional.of(c.identifier());
			case ApplyOperator c -> Optional.of(c.resultName());
			default -> Optional.empty();
		};
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
	 * The file name of {@code source}, or {@code <stdin>} as is: it names no
	 * file, and is not even a valid path on Windows.
	 */
	private static String fileName(String source) {
		return CompilationConfig.STDIN.equals(source)
				? source
				: String.valueOf(Path.of(source).getFileName());
	}
}
