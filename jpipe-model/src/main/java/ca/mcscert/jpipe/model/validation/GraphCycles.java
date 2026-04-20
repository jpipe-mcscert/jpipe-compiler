package ca.mcscert.jpipe.model.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic DFS-based cycle detector for directed graphs.
 *
 * <p>
 * Parameterised over the node type {@code N} so the same algorithm can be
 * reused for structurally different graphs (e.g. support edges represented as
 * {@code Map<String,List<String>>} and implements chains represented as linked
 * {@link ca.mcscert.jpipe.model.JustificationModel} objects).
 */
final class GraphCycles {

	private GraphCycles() {
	}

	/**
	 * Runs a DFS from every node in {@code starts}, detecting back-edges
	 * (cycles).
	 *
	 * <p>
	 * The algorithm maintains two sets per DFS path:
	 * <ul>
	 * <li>{@code globalVisited} — nodes fully processed; prevents redundant
	 * traversals across multiple start nodes. Pass an empty, mutable set; it is
	 * populated in place so callers can share it across multiple {@code detect}
	 * calls on the same graph.
	 * <li>{@code currentPath} — nodes on the current DFS stack; a back-edge is
	 * detected when a successor is already in this set.
	 * </ul>
	 *
	 * <p>
	 * When a back-edge is found, {@code onCycle} is called with the node that
	 * closes the cycle (i.e. the node whose key is already in
	 * {@code currentPath}). Traversal continues after reporting so all cycles
	 * are found, not just the first.
	 *
	 * @param <N>
	 *            node type
	 * @param starts
	 *            initial nodes; each is used as a DFS root if not already
	 *            visited
	 * @param keyOf
	 *            extracts a unique string key from a node; used for
	 *            visited/path membership tests
	 * @param successorsOf
	 *            returns the outgoing neighbours of a node
	 * @param globalVisited
	 *            shared visited set; pass {@code new HashSet<>()} for a fresh
	 *            traversal
	 * @param onCycle
	 *            called with the node whose key is already on the current DFS
	 *            path
	 */
	static <N> void detect(Collection<N> starts, Function<N, String> keyOf,
			Function<N, List<N>> successorsOf, Set<String> globalVisited,
			Consumer<N> onCycle) {
		for (N start : starts) {
			dfs(start, keyOf, successorsOf, globalVisited, new HashSet<>(),
					onCycle);
		}
	}

	private static <N> void dfs(N node, Function<N, String> keyOf,
			Function<N, List<N>> successorsOf, Set<String> globalVisited,
			Set<String> currentPath, Consumer<N> onCycle) {
		String key = keyOf.apply(node);
		if (currentPath.contains(key)) {
			onCycle.accept(node);
			return;
		}
		if (globalVisited.contains(key)) {
			return;
		}
		globalVisited.add(key);
		currentPath.add(key);
		for (N next : successorsOf.apply(node)) {
			dfs(next, keyOf, successorsOf, globalVisited, currentPath, onCycle);
		}
		currentPath.remove(key);
	}
}
