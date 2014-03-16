package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import static joust.utils.StaticCompilerUtils.*;

public class AssertionStrippingTranslator extends TreeTranslator {
    @Override
    public void visitAssert(JCTree.JCAssert jcAssert) {
        result = javacTreeMaker.Skip();
    }
}
