package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;

public class ExpressionNormalisingTranslator extends BaseTranslator  {
    boolean changing = false;

    @Override
    public void visitParens(JCTree.JCParens jcParens) {
        result = jcParens.expr;
        changing = true;
    }

    public boolean makingChanges() {
        return changing;
    }
}
