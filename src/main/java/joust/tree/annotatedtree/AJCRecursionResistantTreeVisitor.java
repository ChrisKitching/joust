package joust.tree.annotatedtree;

import java.util.HashSet;

/**
 * A tree visitor that keeps track of visited node to ensure it never enters an endless loop.
 */
public class AJCRecursionResistantTreeVisitor extends AJCTreeVisitor {
    private final HashSet<AJCTree> visited = new HashSet<AJCTree>();

    @Override
    protected void visit(AJCTree that) {
        if (that == null) {
            return;
        }

        if (visited.contains(that)) {
            return;
        }

        visited.add(that);
        super.visit(that);
    }
}
