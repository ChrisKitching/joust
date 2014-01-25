package joust.optimisers.translators;

import com.sun.tools.javac.tree.TreeTranslator;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * The superclass of all TreeTranslators in JOUST.
 */
public abstract class BaseTranslator extends TreeTranslator {
    // Boolean to track if this visitor has made any changes to the tree this iteration.
    protected boolean mHasMadeAChange;

    // The method declaration enclosing the current execution scope, if any.
    protected JCMethodDecl enclosingMethod;

    /**
     *  Return if this visitor has made any changes to the tree.
     *  While this returns true, further passes are required to simplify everything.
     */
    public boolean makingChanges() {
        return mHasMadeAChange;
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        enclosingMethod = tree;
        super.visitMethodDef(tree);
    }
}
