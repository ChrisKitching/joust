package joust.tree.conversion;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.Gen;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import joust.utils.data.JavacListUtils;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.utils.compiler.StaticCompilerUtils.gen;

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
 * Apply the Gen phase's tree normalisation (Combines  static initialisers into the <clinit> method, shifts initialisers
 * from fields into the constructors, etc.)
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class TreePreparationTranslator extends TreeTranslator {
    private Method normaliseMethod;
    private Field endPosTableField;
    private Field toplevelField;
    public TreePreparationTranslator() {
        // Set up reflective access to some more parts of Javac that need beating with a stick...
        Class<Gen> genClass = Gen.class;
        try {
            normaliseMethod = genClass.getDeclaredMethod("normalizeDefs", List.class, Symbol.ClassSymbol.class);
            normaliseMethod.setAccessible(true);
            endPosTableField = genClass.getDeclaredField("endPosTable");
            endPosTableField.setAccessible(true);
            toplevelField = genClass.getDeclaredField("toplevel");
            toplevelField.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            log.fatal("Exception initialising TreePreparationTranslator!", e);
        }
    }

    private JCCompilationUnit currentToplevel;

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        currentToplevel = tree;
        super.visitTopLevel(tree);
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        List<JCTree> newMethodDefs;
        try {
            toplevelField.set(gen, currentToplevel);
            endPosTableField.set(gen, currentToplevel.endPositions);
            newMethodDefs = (List<JCTree>) normaliseMethod.invoke(gen, jcClassDecl.defs, jcClassDecl.sym);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.fatal("Exception normalising class def!", e);
            return;
        }

        int i = 0;

        for (JCTree t : jcClassDecl.defs) {
            if (t instanceof JCBlock) {
                JCBlock cast = (JCBlock) t;
                if (cast.stats.isEmpty()
                || (cast.flags & Flags.STATIC) != 0) {
                    jcClassDecl.defs = JavacListUtils.removeAtIndex(jcClassDecl.defs, i);
                    i--;
                }
            } else if (t instanceof JCMethodDecl) {
                jcClassDecl.defs = JavacListUtils.removeAtIndex(jcClassDecl.defs, i);
                i--;
            }
            i++;
        }

        jcClassDecl.defs = newMethodDefs.prependList(jcClassDecl.defs);

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
        log.fatal("FOUND A LET EXPRESSION!\n{}", letExpr);
        super.visitLetExpr(letExpr);
    }

    private static JCStatement ensureBlock(JCStatement tree) {
        return ensureBlock(tree, 0);
    }
}
