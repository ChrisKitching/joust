package joust.utils.tree;

import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;

import static joust.tree.annotatedtree.AJCTree.*;

public class CallDetector extends AJCTreeVisitor {
    public boolean containsCall = false;

    @Override
    protected void visitCall(AJCCall that) {
        containsCall = true;
        return;
    }
}
