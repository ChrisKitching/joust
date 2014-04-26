package joust.tree.conversion;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.jvm.Gen;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Symbol.*;
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
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class TreePreparationTranslator extends TreeTranslator {
    private static Method normalizeMethod;
    static {
        try {
            normalizeMethod = Gen.class.getDeclaredMethod("normalizeMethod", JCMethodDecl.class, List.class, List.class);
            normalizeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            log.fatal("Unable to get normaliseMethod method from Gen!", e);
        }
    }

    /**
     * Get the default initialiser expression for a given field type.
     */
    public static JCExpression getDefaultLiteralValueForType(Type t) {
        // Object types default to null.
        if (!t.isPrimitive()) {
            return javacTreeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
        }

        // Numerical types default to zero, booleans to false.
        Type.JCPrimitiveType cast = (Type.JCPrimitiveType) t;
        switch(cast.getTag()) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
                return javacTreeMaker.TypeCast(cast, javacTreeMaker.Literal(0).setType(t));
            case LONG:
                return javacTreeMaker.Literal(0L);
            case FLOAT:
                return javacTreeMaker.Literal(0.0F);
            case DOUBLE:
                return javacTreeMaker.Literal(0.0D);
            case BOOLEAN:
                return javacTreeMaker.Literal(false);
            default:
                return javacTreeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
        }
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        ListBuffer<JCTree> newDefs = new ListBuffer<>();

        ListBuffer<JCStatement> initCode = new ListBuffer<>();

        // Used to find constructors.
        List<JCMethodDecl> constructorDefs = List.nil();

        // Sort definitions into three listbuffers:
        //  - initCode for instance initializers
        //  - clinitCode for class initializers
        //  - methodDefs for method definitions
        for (List<JCTree> l = jcClassDecl.defs; l.nonEmpty(); l = l.tail) {
            JCTree def = l.head;
            switch (def.getTag()) {
                case BLOCK:
                    JCBlock block = (JCBlock)def;
                    if (block.stats.isEmpty()) {
                        continue;
                    }

                    // Add blocks to either the constructor code or the <clinit> code.
                    if ((block.flags & STATIC) == 0) {
                        initCode.append(block);
                    } else {
                        // Quietly ignore static initialisers.
                        newDefs.append(block);
                    }
                    break;
                case METHODDEF:
                    newDefs.append(def);

                    // If this is a ctor, add it to the list of ctors.
                    JCMethodDecl md = (JCMethodDecl) def;

                    if (TreeInfo.isInitialConstructor(md)) {
                        log.debug("Found ctor: {}", md);
                        constructorDefs = constructorDefs.prepend(md);
                    }
                    break;
                case VARDEF:
                    JCVariableDecl vdef = (JCVariableDecl) def;
                    VarSymbol sym = vdef.sym;

                    // Leave statics alone...
                    if ((sym.flags() & STATIC) != 0) {
                        newDefs.append(def);
                        continue;
                    }

                    // So we're going to need to put an initialiser for it somewhere, so let's make it.
                    JCStatement init;

                    if (vdef.init != null) {
                        // Create an assignment equivalent to the action of the initialiser of vdef.
                        init = javacTreeMaker.at(vdef.pos()).Assignment(sym, vdef.init);
                    } else {
                        // Create an assignment for this symbol to the appropriate default value.
                        JCExpression literalValue = getDefaultLiteralValueForType(sym.type);
                        init = javacTreeMaker.at(vdef.pos()).Assignment(sym, literalValue);
                    }

                    // Drop the real initialiser to prevent the *actual* normalisation step from processing it.
                    vdef.init = null;

                    // Add the assignment to the constructor.
                    initCode.append(init);

                    newDefs.append(def);
                    break;
                default:
                    log.fatal("Unknown JCTree class declaration type: {}", def.getTag());
                    break;
            }
        }

        // Insert any instance initializers into all constructors.
        if (!initCode.isEmpty()) {
            List<JCStatement> initStats = initCode.toList();
            for (JCMethodDecl ctor : constructorDefs) {
                try {
                    normalizeMethod.invoke(gen, ctor, initStats, List.nil());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.fatal("Exception normalising method: {}\n\n", ctor, e);
                }
            }
        }

        jcClassDecl.defs = newDefs.toList();

        super.visitClassDef(jcClassDecl);
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


    // Visitors for nodes that should no longer exist in the tree. If they do, it indicates something has happened, such
    // as the compiler being drastically modified, which prevents us from sanely performing our work.
    @Override
    public void visitForeachLoop(JCEnhancedForLoop tree) {
        undigestableNodeType(tree);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter tree) {
        undigestableNodeType(tree);
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        undigestableNodeType(tree);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind tree) {
        undigestableNodeType(tree);
    }

    @Override
    public void visitErroneous(JCErroneous tree) {
        log.error("Erroneous tree encountered - there'll probably be an error from the compiler about that that's more interesting...");
        undigestableNodeType(tree);
    }

    private void undigestableNodeType(JCTree tree) {
        log.fatal("Encountered unexpected tree note type {} of value: {}", tree.getClass().getCanonicalName(), tree);
    }

    private static JCStatement ensureBlock(JCStatement tree) {
        return ensureBlock(tree, 0);
    }
}
