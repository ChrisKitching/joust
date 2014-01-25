package joust.optimisers.translators;

import com.sun.tools.javac.tree.TreeTranslator;

/**
 * A
 */
public abstract class ChangeTrackingTranslator extends TreeTranslator {
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
