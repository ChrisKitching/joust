package joust.optimisers.translators;

import joust.tree.annotatedtree.AJCTreeVisitorImpl;


/**
 * The superclass of all TreeTranslators in JOUST.
 */
public abstract class BaseTranslator extends AJCTreeVisitorImpl {
    // Boolean to track if this visitor has made any changes to the tree this iteration.
    protected boolean mHasMadeAChange;

    /**
     *  Return if this visitor has made any changes to the tree.
     *  While this returns true, further passes are required to simplify everything.
     */
    public boolean makingChanges() {
        return mHasMadeAChange;
    }
}
