package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;

import static joust.Optimiser.treeMaker;

/**
 * A tree translator that removes all assertions from the tree.
 */
public class AssertionStrippingTranslator extends ChangeTrackingTranslator {
    @Override
    public void visitAssert(JCTree.JCAssert jcAssert) {
        super.visitAssert(jcAssert);
        result = treeMaker.Skip();
        mHasMadeAChange = true;
    }
}
