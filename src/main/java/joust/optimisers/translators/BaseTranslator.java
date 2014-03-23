package joust.optimisers.translators;

import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;


/**
 * The superclass of all TreeTranslators in JOUST.
 */
public abstract class BaseTranslator extends AJCTreeVisitor {
    // Boolean to track if this visitor has made any changes to the tree this iteration.
    protected boolean mHasMadeAChange;

    /**
     *  Return if this visitor has made any changes to the tree.
     *  While this returns true, further passes are required to simplify everything.
     */
    public boolean makingChanges() {
        return mHasMadeAChange;
    }

    @Override
    public void visitTree(AJCTree tree) {
        mHasMadeAChange = false;

        super.visitTree(tree);
    }
}
