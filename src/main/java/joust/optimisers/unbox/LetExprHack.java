package joust.optimisers.unbox;

import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;

import static joust.tree.annotatedtree.AJCTree.*;

import static joust.utils.compiler.StaticCompilerUtils.*;

/**
 * Dirty dirty hack to fix LetExpr type annotations
 */
public class LetExprHack extends AJCTreeVisitor {
    @Override
    protected void visitLetExpr(AJCLetExpr that) {
        super.visitLetExpr(that);

        if (!types.isSameType(that.getNodeType(), that.expr.getNodeType())) {
            that.setType(that.expr.getNodeType());
        }
    }
}
