package joust.tree.conversion;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import joust.utils.JCTreeStructurePrinter;
import joust.utils.JavacListUtils;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.utils.StaticCompilerUtils.*;

/**
 * A tree translator to perform translations that simplify the tree prior to converting it to our own representation.
 *
 * Removal of all JCParen nodes from the AST (They just get in the way)
 * Replacing any single-nodes that could be a JCBlock with a JCBlock. Presented with a single-statement
 * while-loop body (for example), javac will have a JCStatement node in the body field of the
 * resulting JCWhile loop - but for a multiple-statement body it'll be a JCBlock. This is annoying,
 * as it means that, in order to process such nodes, one has to first check the runtime type of the
 * node under consideration. (Instead of iterating a single-element block).
 * If enabled, stripping of assertions. No point converting them just to throw them away...
 * Elimination of the empty blocks that javac puts in instead of inner classes..... (WAT).
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class TreePreparationTranslator extends TreeTranslator {
    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        //jcClassDecl.accept(new JCTreeStructurePrinter());

        int i = 0;

        for (JCTree t : jcClassDecl.defs) {
            if (t instanceof JCBlock) {
                jcClassDecl.defs = JavacListUtils.removeAtIndex(jcClassDecl.defs, i);
                i--;
            }
            i++;
        }
        super.visitClassDef(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        super.visitParens(jcParens);
        result = jcParens.expr;
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        super.visitDoLoop(jcDoWhileLoop);
        jcDoWhileLoop.body = ensureBlock(jcDoWhileLoop.body);
        result = jcDoWhileLoop;
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        super.visitWhileLoop(jcWhileLoop);
        jcWhileLoop.body = ensureBlock(jcWhileLoop.body);
        result = jcWhileLoop;
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        super.visitForLoop(jcForLoop);
        jcForLoop.body = ensureBlock(jcForLoop.body);
        result = javacTreeMaker.Block(0, List.<JCStatement>of(jcForLoop));
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        super.visitForeachLoop(jcEnhancedForLoop);
        jcEnhancedForLoop.body = ensureBlock(jcEnhancedForLoop.body);
        result = jcEnhancedForLoop;
    }

    @Override
    public void visitIf(JCIf jcIf) {
        super.visitIf(jcIf);
        jcIf.thenpart = ensureBlock(jcIf.thenpart);
        jcIf.elsepart = ensureBlock(jcIf.elsepart);
        result = jcIf;
    }

    // Throw away type parameters - any that are still around by the time we run are just a javac bug anyway..
    // *cough* enums *cough*.
    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        result = jcTypeApply.clazz;
    }

    /**
     * Ensure that the given statement is a block. If it is not, return the input enclosed in one.
     *
     * @param flags The flags to pass to javacTreeMaker if a block is created.
     */
    private static JCStatement ensureBlock(JCStatement tree, int flags) {
        // If there's no tree, replace it with an empty block.
        if (tree == null) {
            return javacTreeMaker.Block(flags, List.<JCStatement>nil());
        }

        if (tree instanceof JCBlock) {
            return tree;
        }

        return javacTreeMaker.Block(flags, List.of(tree));
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        log.error("FOUND A LET EXPRESSION\n{}", letExpr);

        super.visitLetExpr(letExpr);
    }

    private static JCStatement ensureBlock(JCStatement tree) {
        return ensureBlock(tree, 0);
    }
}
