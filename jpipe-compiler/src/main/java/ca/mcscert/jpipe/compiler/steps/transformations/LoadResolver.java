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
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
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
 * being compiled (taken from the {@link CompilationContext#sourcePath()}). When
 * {@code path} is a glob pattern it expands to every matched file instead of
 * one; its literal prefix is resolved the same way, so a pattern may point
 * below that directory, above it ({@code ../library/*.jd}) or at an absolute
 * location — see {@link #anchor}.</li>
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
	 *            unquoted path to the file to load, or a glob pattern matching
	 *            several of them; resolved relative to the declaring file
	 *            unless absolute.
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
		Set<String> loaded = new HashSet<>();
		return resolve(commands, ctx, visited, loaded);
	}

	// -------------------------------------------------------------------------
	// Load resolution
	// -------------------------------------------------------------------------

	private List<Command> resolve(List<Command> commands,
			CompilationContext ctx, Set<Path> visited, Set<String> loaded) {
		List<Command> result = new ArrayList<>();
		for (Command cmd : commands) {
			if (!(cmd instanceof LoadDirective load)) {
				result.add(cmd);
				continue;
			}
			result.addAll(expand(load, ctx, visited, loaded));
		}
		return result;
	}

	private List<Command> expand(LoadDirective load, CompilationContext ctx,
			Set<Path> visited, Set<String> loaded) {
		logger.debug("Expanding load [{}] as [{}]", load.path(),
				load.namespace());
		Path base = Paths.get(ctx.sourcePath()).toAbsolutePath().normalize()
				.getParent();

		if (!isGlob(load.path())) {
			Path resolved = base.resolve(load.path()).normalize();
			return expandOne(resolved, load, ctx, visited, loaded);
		}
		return expandGlob(load, ctx, base, visited, loaded);
	}

	/**
	 * Expands a glob pattern into the commands of every matched file. The
	 * pattern is first anchored (see {@link #anchor}), so it may reach outside
	 * the declaring file's directory via {@code ..} or an absolute prefix.
	 */
	private List<Command> expandGlob(LoadDirective load, CompilationContext ctx,
			Path base, Set<Path> visited, Set<String> loaded) {
		GlobAnchor anchored = anchor(base, load.path());

		PathMatcher matcher;
		try {
			matcher = FileSystems.getDefault()
					.getPathMatcher("glob:" + anchored.pattern());
		} catch (PatternSyntaxException e) {
			ctx.fatal("Invalid glob in load pattern '" + load.path() + "': "
					+ e.getDescription());
			return List.of();
		}
		if (hasUpwardSegment(anchored.pattern())) {
			ctx.fatal("'..' may only appear before the first wildcard in load"
					+ " pattern '" + load.path() + "'");
			return List.of();
		}
		if (!Files.isDirectory(anchored.root())) {
			ctx.fatal("Cannot expand load pattern '" + load.path() + "': '"
					+ anchored.root() + "' is not a directory");
			return List.of();
		}

		List<Path> matches;
		try {
			matches = matchGlob(anchored.root(), matcher);
		} catch (IOException | UncheckedIOException e) {
			ctx.fatal("Cannot expand load pattern '" + load.path() + "': "
					+ e.getMessage());
			return List.of();
		}
		if (matches.isEmpty()) {
			ctx.fatal("No file matches load pattern '" + load.path() + "'");
			return List.of();
		}
		List<Command> result = new ArrayList<>();
		for (Path resolved : matches) {
			result.addAll(expandOne(resolved, load, ctx, visited, loaded));
		}
		return result;
	}

	/**
	 * Expands a single resolved file into its (recursively resolved and
	 * optionally namespace-prefixed) command list. Cycle detection and
	 * duplicate suppression are performed here, per file, so they work
	 * identically for a literal load and for each match of a glob pattern.
	 */
	private List<Command> expandOne(Path resolved, LoadDirective load,
			CompilationContext ctx, Set<Path> visited, Set<String> loaded) {
		if (visited.contains(resolved)) {
			logger.debug("Circular load detected: [{}], skipping", resolved);
			ctx.fatal("Circular load detected: " + resolved);
			return List.of();
		}

		String loadKey = resolved + "|"
				+ (load.namespace() == null ? "" : load.namespace());
		if (loaded.contains(loadKey)) {
			logger.warn(
					"File '{}' already loaded under namespace '{}', skipping duplicate",
					resolved,
					load.namespace() == null ? "(flat)" : load.namespace());
			return List.of();
		}
		CompilationContext subCtx = new CompilationContext(resolved.toString());
		try {
			List<Command> subCommands = parseFile(resolved, subCtx);

			Set<Path> newVisited = new HashSet<>(visited);
			newVisited.add(resolved);
			subCommands = resolve(subCommands, subCtx, newVisited, loaded);
			if (load.namespace() != null) {
				subCommands = prefix(load.namespace(), subCommands);
			}
			loaded.add(loadKey);
			return subCommands;

		} catch (IOException e) {
			subCtx.fatal("Cannot open loaded file '" + resolved + "': "
					+ e.getMessage());
			return List.of();
		} catch (CompilationException _) {
			// sub-pipeline already recorded its diagnostics in subCtx
			return List.of();
		} finally {
			subCtx.diagnostics().forEach(ctx::report);
		}
	}

	/**
	 * A glob pattern split into the directory the search is anchored at and the
	 * portion of the pattern matched relative to that directory.
	 *
	 * @param root
	 *            absolute, normalized directory to walk.
	 * @param pattern
	 *            glob matched against paths relative to {@code root}.
	 */
	private record GlobAnchor(Path root, String pattern) {
	}

	/**
	 * Index of the first glob metacharacter in {@code path}, or {@code -1} if
	 * it holds none. Single source of truth for what counts as "a glob", shared
	 * by {@link #isGlob} and {@link #anchor}.
	 */
	private static int firstMetaChar(String path) {
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '*' || c == '?' || c == '[' || c == '{') {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tells whether a load path is a glob pattern (rather than a literal path).
	 * A literal path keeps the historical single-file behavior, including the
	 * "Cannot open loaded file" fatal error when it does not exist.
	 */
	private static boolean isGlob(String path) {
		return firstMetaChar(path) >= 0;
	}

	/**
	 * Anchors a glob pattern: everything up to the last {@code /} preceding the
	 * first wildcard is a literal path, resolved against {@code base} exactly
	 * like a literal load, and the remainder is the pattern to match relative
	 * to that directory. This is what lets a pattern reach outside the
	 * declaring file's directory ({@code ../library/*.jd}) or name an absolute
	 * location ({@code /opt/models/*.jd}).
	 *
	 * <p>
	 * The cut is made before the first wildcard <em>character</em>, never
	 * inside a wildcard construct, so a brace group spanning a separator such
	 * as {@code {foo/bar,baz}/*.jd} stays intact. Because matching
	 * {@code dir/X} against {@code dir/<pat>} relative to {@code base} is
	 * equivalent to matching {@code X} against {@code <pat>} relative to
	 * {@code base/dir}, anchoring leaves the meaning of every previously
	 * supported pattern unchanged — it only narrows the subtree that has to be
	 * walked.
	 */
	private static GlobAnchor anchor(Path base, String pattern) {
		int cut = pattern.lastIndexOf('/', firstMetaChar(pattern));
		if (cut < 0) {
			return new GlobAnchor(base, pattern);
		}
		// cut == 0 means an absolute pattern such as "/opt/*.jd": the literal
		// prefix is the root itself, which substring(0, 0) would lose.
		String prefix = cut == 0 ? "/" : pattern.substring(0, cut);
		return new GlobAnchor(base.resolve(prefix).normalize(),
				pattern.substring(cut + 1));
	}

	/**
	 * Tells whether a pattern contains a {@code ..} segment. Directory walking
	 * only ever descends, so a {@code ..} left after anchoring (i.e. one that
	 * follows a wildcard, as in {@code *}{@code /../foo.jd}) can never match
	 * anything; the caller reports it as a FATAL rather than as a puzzling
	 * no-match.
	 */
	private static boolean hasUpwardSegment(String pattern) {
		return Arrays.stream(pattern.split("/", -1)).anyMatch(".."::equals);
	}

	/**
	 * Walks {@code root}, returning the regular files matching {@code matcher}
	 * as absolute, normalized paths in a deterministic (lexicographic) order.
	 * Standard glob semantics apply: {@code *} does not cross directory
	 * boundaries, while {@code **} does.
	 *
	 * @throws IOException
	 *             if walking {@code root} fails; reported as a FATAL by the
	 *             caller so an IO failure is not misclassified as a no-match.
	 */
	private static List<Path> matchGlob(Path root, PathMatcher matcher)
			throws IOException {
		try (Stream<Path> walk = Files.walk(root)) {
			return walk.filter(Files::isRegularFile)
					.filter(p -> matcher.matches(root.relativize(p)))
					.map(p -> p.toAbsolutePath().normalize()).sorted().toList();
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

	private static String qualify(String ns, String name) {
		return ns + ":" + name;
	}

	private static Command rewrite(String ns, Command cmd) {
		return switch (cmd) {
			case CreateJustification c -> new CreateJustification(
					qualify(ns, c.identifier()), c.location());
			case CreateTemplate c ->
				new CreateTemplate(qualify(ns, c.identifier()), c.location());
			case CreateConclusion c ->
				new CreateConclusion(qualify(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case CreateEvidence c ->
				new CreateEvidence(qualify(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case CreateStrategy c ->
				new CreateStrategy(qualify(ns, c.container()), c.identifier(),
						c.label(), c.location());
			case CreateSubConclusion c ->
				new CreateSubConclusion(qualify(ns, c.container()),
						c.identifier(), c.label(), c.location());
			case CreateAbstractSupport c ->
				new CreateAbstractSupport(qualify(ns, c.container()),
						c.identifier(), c.label(), c.location());
			case ImplementsTemplate c ->
				new ImplementsTemplate(qualify(ns, c.modelName()),
						qualify(ns, c.templateName()), c.location());
			case AddSupport c -> new AddSupport(qualify(ns, c.container()),
					c.supportableId(), c.supporterId(), c.location());
			case OverrideAbstractSupport c ->
				new OverrideAbstractSupport(qualify(ns, c.container()),
						c.qualifiedId(), c.newType(), c.label(), c.location());
			default -> cmd;
		};
	}
}
