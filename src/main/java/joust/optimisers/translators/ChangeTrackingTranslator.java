package joust.optimisers.translators;

/**
 * A
 */
public abstract class ChangeTrackingTranslator extends BaseTranslator {
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
