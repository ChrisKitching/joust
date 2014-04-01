package joust.tree.annotatedtree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Scan a Java AST and convert it to the annotated format. Relies on the iteration order of TreeScanner.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class InitialASTConverter extends TreeScanner {
    // The 0-length String[]...
    public static final String[] NONE = {};
    // This horror is a mapping from new tree classes to the lists of field names they need copied from the source
    // AST node, in the order they must be popped from the stack to do so successfully. (The reverse of the order that
    // TreeScanner visits them.
    private static HashMap<Class<? extends AJCTree>, String[]> FIELD_MAPPINGS;

     /**
     * Populate the field mappings. Allows us to ensure we only keep the structure in memory while it remains useful.
     */
    public static void init() {
        FIELD_MAPPINGS = new HashMap<Class<? extends AJCTree>, String[]>() {
            {
                put(AJCConditional.class, new String[] {"falsepart", "truepart", "cond"});
                put(AJCCall.class, new String[] {"args", "meth"});
                put(AJCNewClass.class, new String[] {"def", "args", "clazz"});
                put(AJCClassDecl.class, new String[] {"implementing", "extending", "mods"});
                put(AJCMethodDecl.class, new String[] {"body", "defaultValue", "thrown", "params", "recvparam", "mods"});
                put(AJCVariableDecl.class, new String[] {"init", "mods"});
                put(AJCSkip.class, NONE);
                put(AJCBlock.class, new String[] {"stats"});
                put(AJCDoWhileLoop.class, new String[] {"cond", "body"});
                put(AJCWhileLoop.class, new String[] {"body", "cond"});
                put(AJCForLoop.class, new String[] {"body", "step", "cond", "init"});
                put(AJCLabeledStatement.class, new String[] {"body"});
                put(AJCSwitch.class, new String[] {"cases", "selector"});
                put(AJCCase.class, new String[] {"stats", "pat"});
                put(AJCSynchronized.class, new String[] {"body", "lock"});
                put(AJCTry.class, new String[] {"finalizer", "catchers", "body"});
                put(AJCCatch.class, new String[] {"body", "param"});
                put(AJCCatch.class, new String[] {"body", "param"});
                put(AJCIf.class, new String[] {"elsepart", "thenpart", "cond"});
                put(AJCExpressionStatement.class, new String[] {"expr"});
                put(AJCBreak.class, NONE);
                put(AJCContinue.class, NONE);
                put(AJCReturn.class, new String[] {"expr"});
                put(AJCNewArray.class, new String[] {"elems", "dims", "annotations"});
                put(AJCAssign.class, new String[] {"rhs", "lhs"});
                put(AJCAssignOp.class, new String[] {"rhs", "lhs"});
                put(AJCUnary.class, new String[] {"arg"});
                put(AJCUnaryAsg.class, new String[] {"arg"});
                put(AJCBinary.class, new String[] {"rhs", "lhs"});
                put(AJCTypeCast.class, new String[] {"expr"});
                put(AJCInstanceOf.class, new String[] {"clazz", "expr"});
                put(AJCArrayAccess.class, new String[] {"index", "indexed"});
                put(AJCFieldAccess.class, new String[] {"selected"});
                put(AJCIdent.class, NONE);
                put(AJCLiteral.class, NONE);
                put(AJCPrimitiveTypeTree.class, NONE);
                put(AJCArrayTypeTree.class, new String[] {"elemtype"});
                put(AJCTypeUnion.class, new String[] {"alternatives"});
                put(AJCAnnotation.class, new String[] {"args", "annotationType"});
                put(AJCModifiers.class, new String[] {"annotations"});
                put(AJCAnnotatedType.class, new String[] {"underlyingType"});
                put(AJCErroneous.class, NONE);
                put(AJCLetExpr.class, new String[] {"expr", "defs"});
                put(AJCThrow.class, new String[] {"expr"});
            }
        };
    }

    public static void uninit() {
        FIELD_MAPPINGS = null;
    }

    // The method node enclosing the nodes being processed, if any.
    private AJCMethodDecl enclosingMethod;

    // The current enclosing block, if any.
    private AJCBlock enclosingBlock;

    // Intermediate results...
    private final Stack<AJCTree> results = new Stack<>();

    /**
     * Extract the final result from the converter after all processing has been completed.
     * The input tree must be fed to this scanner before calling this for the result to be a victory...
     */
    public AJCTree getResult() {
        AJCTree result =  results.pop();
        if (!results.isEmpty()) {
            log.fatal("Failed to convert input tree. Result remaining: " + results.pop() + " and " + results.size() + " more.");
        }

        return result;
    }

    /**
     * Evil reflective function to insulate me against confusingly-null fields in JCTree representations.
     *
     * Given a destination AJCTree, a source JCTree from which information is being copied and a field name,
     * assuming that all children of the sourceTree have been processed, we're ready to build the destination
     * tree from the stuff on the results stack...
     * ... But what if something was null?
     *
     * Since the rules about which fields on JCTree nodes may or may not be null are obtuse, this function exists.
     * Given a field name, check if that field is null on sourceTree. If not, pull something off the stack
     * and assign it to the destintionTree field by that name.
     */
    private void setFields(AJCTree destinationTree, JCTree sourceTree) {
        Class<? extends AJCTree> destClass = destinationTree.getClass();
        Class<? extends JCTree> sourceClass = sourceTree.getClass();

//        log.debug("Setting fields for {}:{}", sourceTree, sourceTree.getClass().getCanonicalName());
//        int ii = 0;
//        for (AJCTree t : results) {
//            log.debug("{} : {}", ii, t);
//            ii++;
//        }

        String[] fieldNames = FIELD_MAPPINGS.get(destClass);
        if (fieldNames == null) {
            log.fatal("Unable to find field mappings for class: " + destClass.getCanonicalName());
            return;
        }

        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            try {
                Field destField = destClass.getDeclaredField(fieldName);
                destField.setAccessible(true);
                Field sourceField = sourceClass.getDeclaredField(fieldName);
                sourceField.setAccessible(true);

                Object o = sourceField.get(sourceTree);

                // Determine the type of the source field. If it's a list, we need more magic.
                Class<?> fieldType = sourceField.getType();

                if (o == null) {
                    // Use empty lists instead of null lists...
                    if ("com.sun.tools.javac.util.List".equals(fieldType.getCanonicalName())) {
                        destField.set(destinationTree, List.nil());
                    }
                    continue;
                }

                log.debug("Field {} : {}", fieldName, fieldType.getCanonicalName());
                if (!"com.sun.tools.javac.util.List".equals(fieldType.getCanonicalName())) {
                    AJCTree value = results.pop();
                    log.debug("Assigning: {}:{}", value, value.getClass().getCanonicalName());
                    value.mParentNode = destinationTree;
                    destField.set(destinationTree, value);
                    continue;
                }

                // Find out how long the list is on the source object and pull that many elements off the stack.
                List<?> thatList = (List<?>) o;

                int neededElements = thatList.size();

                log.debug("List size: {}", neededElements);

                // The easy way out.
                if (neededElements == 0) {
                    destField.set(destinationTree, List.nil());
                    continue;
                }

                // Is this not a list of lists?
                if (!"com.sun.tools.javac.util.List".equals(thatList.get(0).getClass().getCanonicalName())) {
                    List<?> targetList = listFromIntermediatesWithParent(neededElements, destinationTree);
                    log.debug("-> {}  ({})", Arrays.toString(targetList.toArray()), neededElements);

                    // Finally, assign the list to the field.
                    destField.set(destinationTree, targetList);
                    continue;
                }

                log.fatal("List of lists encountered!");
            } catch (NoSuchFieldException e) {
                log.fatal("Unable to find field " + fieldName + " on " + destClass.getSimpleName(), e);
            } catch (IllegalAccessException e) {
                log.fatal("IllegalAccessException during tree conversion.", e);
            }
        }
    }

    private List<AJCTree> listFromIntermediatesWithParent(int length, AJCTree parent) {
        List<AJCTree> list = List.nil();
        while (length > 0) {
            AJCTree value = results.pop();
            value.mParentNode = parent;
            list = list.prepend(value);
            length--;
        }

        return list;
    }

    // Visitor methods...
    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        scan(jcClassDecl.mods);
        // Explicitly ignore type parameters...
        scan(jcClassDecl.extending);
        scan(jcClassDecl.implementing);
        scan(jcClassDecl.defs);

        AJCClassDecl classDecl = new AJCClassDecl(jcClassDecl);

        // Split the defs by hand...
        List<AJCVariableDecl> varDefs = List.nil();
        List<AJCMethodDecl> methodDefs = List.nil();
        List<AJCClassDecl> classDefs = List.nil();

        int requiredDefs = jcClassDecl.defs.size();
        while (requiredDefs > 0) {
            AJCTree decl = results.pop();
            if (decl instanceof AJCVariableDecl) {
                varDefs = varDefs.prepend((AJCVariableDecl) decl);
            } else if (decl instanceof AJCMethodDecl) {
                methodDefs = methodDefs.prepend((AJCMethodDecl) decl);
            } else if (decl instanceof AJCClassDecl) {
                classDefs = classDefs.prepend((AJCClassDecl) decl);
            } else {
                log.fatal("Unknown definition type: " + decl.getClass().getCanonicalName());
                return;
            }

            requiredDefs--;
        }

        classDecl.fields = varDefs;
        classDecl.methods = methodDefs;
        classDecl.classes = classDefs;

        dumpStack();
        // Copy the remainder - those fields aren't differently structured.
        setFields(classDecl, jcClassDecl);

        results.push(classDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        // Make the object despite the fact we can't populate it just yet - the clients will point to it
        // just the same.
        enclosingMethod = new AJCMethodDecl(jcMethodDecl);

        scan(jcMethodDecl.mods);
        scan(jcMethodDecl.recvparam);
        scan(jcMethodDecl.params);
        scan(jcMethodDecl.thrown);
        scan(jcMethodDecl.defaultValue);
        scan(jcMethodDecl.body);
        if (jcMethodDecl.restype != null) {
            scan(jcMethodDecl.restype);

            enclosingMethod.restype = convertToTypeExpression(results.pop());
        }


        // Now pull in the results...
        setFields(enclosingMethod, jcMethodDecl);
        results.push(enclosingMethod);
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        log.debug("Visiting: {}", tree);
        scan(tree.mods);
        scan(tree.init);
        scan(tree.vartype);

        AJCVariableDecl variableDecl = new AJCVariableDecl(tree);

        // The type will be on the top of the stack...
        variableDecl.vartype = convertToTypeExpression(results.pop());

        variableDecl.enclosingBlock = enclosingBlock;

        setFields(variableDecl, tree);

        results.push(variableDecl);
    }

    /**
     * Convert a given AJCTree node (Generally of type AJCIdent or AJCPrimitiveTypeTree to a type expression, if required.)
     */
    @SuppressWarnings("unchecked")
    public AJCTypeExpression convertToTypeExpression(AJCTree tree) {
        if (tree instanceof AJCPrimitiveTypeTree) {
            return (AJCPrimitiveTypeTree) tree;
        }

        if (tree instanceof AJCArrayTypeTree) {
            return (AJCArrayTypeTree) tree;
        }

        if (tree instanceof AJCSymbolRefTree) {
            return new AJCObjectTypeTree((AJCSymbolRefTree<Symbol.TypeSymbol>) tree);
        }

        log.fatal("Unexpected type expression node type: " + tree + ':' + tree.getClass().getCanonicalName());

        return null;
    }
    public List<AJCTypeExpression> convertToTypeExpression(List<AJCTree> trees) {
        List<AJCTypeExpression> ret = List.nil();

        for (AJCTree t : trees) {
            ret = ret.prepend(convertToTypeExpression(t));
        }

        return ret;
    }

    @Override
    public void visitSkip(JCSkip jcSkip) {
        super.visitSkip(jcSkip);
        AJCSkip result = new AJCSkip(jcSkip);
        result.enclosingBlock = enclosingBlock;
        results.push(result);
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        AJCBlock block = new AJCBlock(jcBlock);
        block.enclosingBlock = enclosingBlock;
        enclosingBlock = block;
        super.visitBlock(jcBlock);

        block.enclosingMethod = enclosingMethod;

        setFields(block, jcBlock);
        results.push(block);
        enclosingBlock = block.enclosingBlock;
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        super.visitDoLoop(jcDoWhileLoop);

        AJCDoWhileLoop node = new AJCDoWhileLoop(jcDoWhileLoop);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcDoWhileLoop);
        results.push(node);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        super.visitWhileLoop(jcWhileLoop);

        AJCWhileLoop node = new AJCWhileLoop(jcWhileLoop);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcWhileLoop);
        results.push(node);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        super.visitForLoop(jcForLoop);

        AJCForLoop node = new AJCForLoop(jcForLoop);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcForLoop);
        results.push(node);
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        super.visitLabelled(jcLabeledStatement);

        AJCLabeledStatement statement = new AJCLabeledStatement(jcLabeledStatement);
        statement.enclosingBlock = enclosingBlock;
        setFields(statement, jcLabeledStatement);
        results.push(statement);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        super.visitSwitch(jcSwitch);

        AJCSwitch node = new AJCSwitch(jcSwitch);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcSwitch);
        results.push(node);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        super.visitCase(jcCase);

        AJCCase node = new AJCCase(jcCase);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcCase);
        results.push(node);
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        super.visitSynchronized(jcSynchronized);

        AJCSynchronized node = new AJCSynchronized(jcSynchronized);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcSynchronized);
        results.push(node);
    }

    @Override
    public void visitTry(JCTry jcTry) {
        super.visitTry(jcTry);

        AJCTry node = new AJCTry(jcTry);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcTry);

        if (node.finalizer == null) {
            node.finalizer = treeMaker.Block(0L, List.<AJCStatement>nil());
        }
        results.push(node);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        super.visitCatch(jcCatch);

        AJCCatch node = new AJCCatch(jcCatch);
        setFields(node, jcCatch);
        results.push(node);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        super.visitConditional(jcConditional);

        AJCConditional node = new AJCConditional(jcConditional);
        setFields(node, jcConditional);
        results.push(node);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        super.visitIf(jcIf);

        AJCIf node = new AJCIf(jcIf);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcIf);
        results.push(node);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        super.visitExec(jcExpressionStatement);

        AJCExpressionStatement node = new AJCExpressionStatement(jcExpressionStatement);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcExpressionStatement);
        results.push(node);
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        super.visitBreak(jcBreak);

        AJCBreak node = new AJCBreak(jcBreak);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcBreak);
        results.push(node);
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        super.visitContinue(jcContinue);

        AJCContinue node = new AJCContinue(jcContinue);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcContinue);
        results.push(node);
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        super.visitReturn(jcReturn);

        AJCReturn node = new AJCReturn(jcReturn);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcReturn);

        if (node.expr == null) {
            node.expr = treeMaker.EmptyExpression();
        }
        results.push(node);
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        super.visitThrow(jcThrow);

        AJCThrow node = new AJCThrow(jcThrow);
        node.enclosingBlock = enclosingBlock;
        setFields(node, jcThrow);
        results.push(node);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        super.visitApply(jcMethodInvocation);

        AJCCall node = new AJCCall(jcMethodInvocation);
        setFields(node, jcMethodInvocation);
        results.push(node);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        super.visitNewClass(jcNewClass);

        AJCNewClass node = new AJCNewClass(jcNewClass);
        setFields(node, jcNewClass);
        results.push(node);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        if (jcNewArray == null) {
            return;
        }

        scan(jcNewArray.annotations);
        scan(jcNewArray.dims);
        scan(jcNewArray.elems);

        // In the event that this is a {...} style declaration (ie. One with elements) then elemtype is null.
        // Otherwise, it's the type of the elements.

        // THIS IS STUPID, JAVAC.
        scan(jcNewArray.elemtype);

        AJCNewArray node = new AJCNewArray(jcNewArray);

        if (jcNewArray.elemtype != null) {
            node.elemtype = convertToTypeExpression(results.pop());
        }
        setFields(node, jcNewArray);

        results.push(node);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        super.visitAssign(jcAssign);

        AJCAssign node = new AJCAssign(jcAssign);
        setFields(node, jcAssign);
        results.push(node);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        AJCAssignOp node = new AJCAssignOp(jcAssignOp);
        setFields(node, jcAssignOp);
        results.push(node);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        super.visitUnary(jcUnary);

        AJCExpressionTree node;

        final Tag nodeTag = jcUnary.getTag();
        if (nodeTag == Tag.PREINC
         || nodeTag == Tag.PREDEC
         || nodeTag == Tag.POSTINC
         || nodeTag == Tag.POSTDEC) {
            node = new AJCUnaryAsg(jcUnary);
        } else {
            node = new AJCUnary(jcUnary);
        }

        setFields(node, jcUnary);
        results.push(node);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        super.visitBinary(jcBinary);

        AJCBinary node = new AJCBinary(jcBinary);
        setFields(node, jcBinary);
        results.push(node);
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        scan(tree.expr);
        scan(tree.clazz);

        AJCTypeCast node = new AJCTypeCast(tree);
        node.clazz = convertToTypeExpression(results.pop());

        setFields(node, tree);
        results.push(node);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        super.visitTypeTest(jcInstanceOf);

        AJCInstanceOf node = new AJCInstanceOf(jcInstanceOf);
        setFields(node, jcInstanceOf);
        results.push(node);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        super.visitIndexed(jcArrayAccess);

        AJCArrayAccess node = new AJCArrayAccess(jcArrayAccess);
        setFields(node, jcArrayAccess);
        results.push(node);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        super.visitSelect(jcFieldAccess);

        AJCFieldAccess node = new AJCFieldAccess(jcFieldAccess);
        setFields(node, jcFieldAccess);
        results.push(node);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        super.visitIdent(jcIdent);

        AJCIdent node = new AJCIdent(jcIdent);
        setFields(node, jcIdent);
        results.push(node);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        super.visitLiteral(jcLiteral);

        AJCLiteral node = new AJCLiteral(jcLiteral);
        setFields(node, jcLiteral);
        results.push(node);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);

        AJCPrimitiveTypeTree node = new AJCPrimitiveTypeTree(jcPrimitiveTypeTree);
        setFields(node, jcPrimitiveTypeTree);
        results.push(node);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);

        AJCArrayTypeTree node = new AJCArrayTypeTree(jcArrayTypeTree);
        node.elemtype = convertToTypeExpression(results.pop());

        log.debug("Type array pushes: {}", node);

        results.push(node);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        super.visitTypeUnion(jcTypeUnion);

        AJCTypeUnion node = new AJCTypeUnion(jcTypeUnion);
        // TODO: Something something list conversion.

        setFields(node, jcTypeUnion);
        results.push(node);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);

        AJCAnnotation node = new AJCAnnotation(jcAnnotation);
        setFields(node, jcAnnotation);
        results.push(node);
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        super.visitModifiers(jcModifiers);

        AJCModifiers node = new AJCModifiers(jcModifiers);
        setFields(node, jcModifiers);
        results.push(node);
        log.debug("Pushing modifiers: {}", node);
    }

    private void dumpStack() {
        for (int i = 1; i < 10; i++) {
            if (i > results.size()) {
                break;
            }
            log.info("[{}] {}:{}", i, results.get(results.size() - i), results.get(results.size() - i).getClass().getCanonicalName());
        }
    }


    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        super.visitAnnotatedType(jcAnnotatedType);

        AJCAnnotatedType node = new AJCAnnotatedType(jcAnnotatedType);
        setFields(node, jcAnnotatedType);
        results.push(node);
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        super.visitErroneous(jcErroneous);

        AJCErroneous node = new AJCErroneous(jcErroneous);
        setFields(node, jcErroneous);
        results.push(node);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        super.visitLetExpr(letExpr);

        AJCLetExpr node = new AJCLetExpr(letExpr);
        setFields(node, letExpr);
        results.push(node);
    }
}
