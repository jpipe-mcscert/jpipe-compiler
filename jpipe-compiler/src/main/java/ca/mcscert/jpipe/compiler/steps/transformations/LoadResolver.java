package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ca.mcscert.jpipe.compiler.steps.checkers.HaltAndCatchFire;
import ca.mcscert.jpipe.model.Unit;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Eliminates all {@link LoadDirective}s from the command list by recursively
 * parsing each referenced file and splicing its prefixed commands in place.
 *
 * <p>
 * {@link LoadResolver} runs after {@link ActionListProvider} and before
 * {@link ActionListInterpretation}, so the
 * {@link ca.mcscert.jpipe.commands.ExecutionEngine} always receives a flat list
 * that contains no {@link LoadDirective}s.
 *
 * <p>
 * Algorithm for each {@code load "path" as ns} directive found in the list:
 * <ol>
 * <li>Resolve {@code path} relative to the directory of the file currently
 * being compiled (taken from the {@link CompilationContext#sourcePath()}).</li>
 * <li>Detect cycles: if the resolved path is already being compiled in the
 * current call stack, report a FATAL and skip.</li>
 * <li>Parse the referenced file up to and including {@link ActionListProvider}
 * using a fresh {@link CompilationContext} tied to the sub-file.</li>
 * <li>Recursively resolve any {@link LoadDirective}s found in that
 * sub-list.</li>
 * <li>Prefix every model name in the expanded sub-list with {@code ns + ":"}
 * via {@link #prefix}.</li>
 * <li>Splice the prefixed commands into the result in place of the
 * {@link LoadDirective}.</li>
 * </ol>
 *
 * <p>
 * Diagnostics produced while compiling a sub-file are always forwarded to the
 * parent {@link CompilationContext}, so the caller sees a unified error report.
 */
public final class LoadResolver
		extends
			Transformation<List<Command>, List<Command>> {

	private static final Logger logger = LogManager.getLogger();

	private final OperatorRegistry operators;
	private final UnificationEquivalenceRegistry unificationEquivalences;

	public LoadResolver(OperatorRegistry operators,
			UnificationEquivalenceRegistry unificationEquivalences) {
		this.operators = operators;
		this.unificationEquivalences = unificationEquivalences;
	}

	/**
	 * A compiler-internal directive produced by the {@code load} grammar rule.
	 * It is consumed and eliminated by {@link LoadResolver} before the command
	 * list reaches {@link ca.mcscert.jpipe.commands.ExecutionEngine}. Its
	 * {@link #condition()} always returns {@code false} and {@link #execute()}
	 * always throws, so it can never be executed directly.
	 *
	 * @param path
	 *            unquoted path to the file to load, relative to the declaring
	 *            file.
	 * @param namespace
	 *            alias under which the loaded file's models are registered, or
	 *            {@code null} for a flat (no-prefix) import.
	 */
	record LoadDirective(String path, String namespace) implements Command {

		@Override
		public Predicate<Unit> condition() {
			return unit -> false;
		}

		@Override
		public void execute(Unit context) {
			throw new UnsupportedOperationException(
					"LoadDirective must be expanded by LoadResolver,"
							+ " not executed directly");
		}
	}

	// -------------------------------------------------------------------------
	// Transformation entry point
	// -------------------------------------------------------------------------

	@Override
	protected List<Command> run(List<Command> commands,
			CompilationContext ctx) {
		Set<Path> visited = new HashSet<>();
		visited.add(Paths.get(ctx.sourcePath()).toAbsolutePath().normalize());
		return resolve(commands, ctx, visited);
	}

	// -------------------------------------------------------------------------
	// Load resolution
	// -------------------------------------------------------------------------

	private List<Command> resolve(List<Command> commands,
			CompilationContext ctx, Set<Path> visited) {
		List<Command> result = new ArrayList<>();
		for (Command cmd : commands) {
			if (!(cmd instanceof LoadDirective load)) {
				result.add(cmd);
				continue;
			}
			result.addAll(expand(load, ctx, visited));
		}
		return result;
	}

	private List<Command> expand(LoadDirective load, CompilationContext ctx,
			Set<Path> visited) {
		logger.debug("Expanding load [{}] as [{}]", load.path(),
				load.namespace());
		Path resolved = Paths.get(ctx.sourcePath()).toAbsolutePath().normalize()
				.getParent().resolve(load.path()).normalize();

		if (visited.contains(resolved)) {
			logger.debug("Circular load detected: [{}], skipping", resolved);
			ctx.fatal("Circular load detected: " + resolved);
			return List.of();
		}

		CompilationContext subCtx = new CompilationContext(resolved.toString());
		try {
			List<Command> subCommands = parseFile(resolved, subCtx);

			Set<Path> newVisited = new HashSet<>(visited);
			newVisited.add(resolved);
			subCommands = resolve(subCommands, subCtx, newVisited);
			if (load.namespace() != null) {
				subCommands = prefix(load.namespace(), subCommands);
			}
			return subCommands;

		} catch (IOException e) {
			subCtx.fatal("Cannot open loaded file '" + resolved + "': "
					+ e.getMessage());
			return List.of();
		} catch (CompilationException e) {
			// sub-pipeline already recorded its diagnostics in subCtx
			return List.of();
		} finally {
			subCtx.diagnostics().forEach(ctx::report);
		}
	}

	/**
	 * Runs the raw parsing chain (up to and including
	 * {@link ActionListProvider}) on a file. This chain does <em>not</em>
	 * include {@link LoadResolver} itself, preventing infinite recursion.
	 * Nested loads in the sub-file are handled by the recursive
	 * {@link #resolve} call after this method returns.
	 */
	private List<Command> parseFile(Path path, CompilationContext subCtx)
			throws IOException {
		Transformation<InputStream, List<Command>> chain = new CharStreamProvider()
				.andThen(new Lexer()).andThen(new Parser())
				.andThen(new HaltAndCatchFire<ParseTree>())
				.andThen(new ActionListProvider(operators,
						unificationEquivalences));
		try (FileInputStream fis = new FileInputStream(path.toFile())) {
			return chain.fire(fis, subCtx);
		}
	}

	// -------------------------------------------------------------------------
	// Command prefixing
	// -------------------------------------------------------------------------

	/**
	 * Returns a new list in which every model-name argument of every command is
	 * prefixed with {@code namespace + ":"}.
	 *
	 * <p>
	 * Only model names (container references and model identifiers) are
	 * prefixed. Element IDs, display labels, and type discriminators are left
	 * untouched: element IDs are local to their model and get qualified by
	 * {@link ca.mcscert.jpipe.model.JustificationModel#inline} at template
	 * expansion time. {@link LoadDirective}s and unrecognised command types are
	 * passed through unchanged.
	 */
	static List<Command> prefix(String namespace, List<Command> commands) {
		List<Command> result = new ArrayList<>(commands.size());
		for (Command cmd : commands) {
			result.add(rewrite(namespace, cmd));
		}
		return result;
	}

	private static String p(String ns, String name) {
		return ns + ":" + name;
	}

	private static Command rewrite(String ns, Command cmd) {
		return switch (cmd) {
			case CreateJustification c ->
				new CreateJustification(p(ns, c.identifier()), c.location());
			case CreateTemplate c ->
				new CreateTemplate(p(ns, c.identifier()), c.location());
			case CreateConclusion c ->
				new CreateConclusion(p(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case CreateEvidence c -> new CreateEvidence(p(ns, c.container()),
					c.identifier(), c.label(), c.location());
			case CreateStrategy c -> new CreateStrategy(p(ns, c.container()),
					c.identifier(), c.label(), c.location());
			case CreateSubConclusion c ->
				new CreateSubConclusion(p(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case CreateAbstractSupport c ->
				new CreateAbstractSupport(p(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case ImplementsTemplate c ->
				new ImplementsTemplate(p(ns, c.modelName()),
						p(ns, c.templateName()), c.location());
			case AddSupport c -> new AddSupport(p(ns, c.container()),
					c.supportableId(), c.supporterId(), c.location());
			case OverrideAbstractSupport c ->
				new OverrideAbstractSupport(p(ns, c.container()),
						c.qualifiedId(), c.newType(), c.label(), c.location());
			default -> cmd;
		};
	}
}
