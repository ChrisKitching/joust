package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;

public class ExpressionNormalisingTranslator extends BaseTranslator  {
    @Override
    public void visitParens(JCTree.JCParens jcParens) {
        result = jcParens.expr;
    }
}
