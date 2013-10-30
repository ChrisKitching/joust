package joust;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import static joust.Optimiser.treeMaker;

/**
 * A dummy module to replace all `if (something) {stuff}` statements with `if (true) {stuff}`.
 * For science.
 */
public class IfStatementTrueifier extends TreeTranslator {
    @Override
    public void visitIf(JCTree.JCIf tree) {
        super.visitIf(tree);

        JCTree.JCStatement thenExpr = tree.getThenStatement();

        // Else is omitted, since it will no longer ever be executed.
        result = treeMaker.If(treeMaker.Literal(true),
                              thenExpr,
                              null);
    }
}
