package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.optimisers.utils.JavacListUtils;
import lombok.extern.log4j.Log4j2;

import java.util.LinkedList;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A TreeTranslator storing a stack of the nodes between the current node and the root.
 * Allows translators to access the properties of parent nodes of interest.
 *
 * Unfortunately, the resulting code is horribly, horribly, horrible.
 */
public @Log4j2
class ParentTrackingTreeTranslator extends BaseTranslator {
    protected LinkedList<JCTree> visitedStack = new LinkedList<>();

    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        visitedStack.push(jcCompilationUnit);
        super.visitTopLevel(jcCompilationUnit);
        visitedStack.pop();
    }

    @Override
    public void visitImport(JCImport jcImport) {
        visitedStack.push(jcImport);
        super.visitImport(jcImport);
        visitedStack.pop();
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        visitedStack.push(jcClassDecl);
        super.visitClassDef(jcClassDecl);
        visitedStack.pop();
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        visitedStack.push(jcMethodDecl);
        super.visitMethodDef(jcMethodDecl);
        visitedStack.pop();
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        visitedStack.push(jcVariableDecl);
        super.visitVarDef(jcVariableDecl);
        visitedStack.pop();
    }

    @Override
    public void visitSkip(JCSkip jcSkip) {
        visitedStack.push(jcSkip);
        super.visitSkip(jcSkip);
        visitedStack.pop();
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        visitedStack.push(jcBlock);
        super.visitBlock(jcBlock);
        visitedStack.pop();
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        visitedStack.push(jcDoWhileLoop);
        super.visitDoLoop(jcDoWhileLoop);
        visitedStack.pop();
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        visitedStack.push(jcWhileLoop);
        super.visitWhileLoop(jcWhileLoop);
        visitedStack.pop();
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        visitedStack.push(jcForLoop);
        super.visitForLoop(jcForLoop);
        visitedStack.pop();
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        visitedStack.push(jcEnhancedForLoop);
        super.visitForeachLoop(jcEnhancedForLoop);
        visitedStack.pop();
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        visitedStack.push(jcLabeledStatement);
        super.visitLabelled(jcLabeledStatement);
        visitedStack.pop();
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        visitedStack.push(jcSwitch);
        super.visitSwitch(jcSwitch);
        visitedStack.pop();
    }

    @Override
    public void visitCase(JCCase jcCase) {
        visitedStack.push(jcCase);
        super.visitCase(jcCase);
        visitedStack.pop();
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        visitedStack.push(jcSynchronized);
        super.visitSynchronized(jcSynchronized);
        visitedStack.pop();
    }

    @Override
    public void visitTry(JCTry jcTry) {
        visitedStack.push(jcTry);
        super.visitTry(jcTry);
        visitedStack.pop();
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        visitedStack.push(jcCatch);
        super.visitCatch(jcCatch);
        visitedStack.pop();
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        visitedStack.push(jcConditional);
        super.visitConditional(jcConditional);
        visitedStack.pop();
    }

    @Override
    public void visitIf(JCIf jcIf) {
        visitedStack.push(jcIf);
        super.visitIf(jcIf);
        visitedStack.pop();
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        visitedStack.push(jcExpressionStatement);
        super.visitExec(jcExpressionStatement);
        visitedStack.pop();
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        visitedStack.push(jcBreak);
        super.visitBreak(jcBreak);
        visitedStack.pop();
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        visitedStack.push(jcContinue);
        super.visitContinue(jcContinue);
        visitedStack.pop();
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        visitedStack.push(jcReturn);
        super.visitReturn(jcReturn);
        visitedStack.pop();
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        visitedStack.push(jcThrow);
        super.visitThrow(jcThrow);
        visitedStack.pop();
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        visitedStack.push(jcAssert);
        super.visitAssert(jcAssert);
        visitedStack.pop();
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        visitedStack.push(jcMethodInvocation);
        super.visitApply(jcMethodInvocation);
        visitedStack.pop();
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        visitedStack.push(jcNewClass);
        super.visitNewClass(jcNewClass);
        visitedStack.pop();
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        visitedStack.push(jcLambda);
        super.visitLambda(jcLambda);
        visitedStack.pop();
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        visitedStack.push(jcNewArray);
        super.visitNewArray(jcNewArray);
        visitedStack.pop();
    }

    @Override
    public void visitParens(JCParens jcParens) {
        visitedStack.push(jcParens);
        super.visitParens(jcParens);
        visitedStack.pop();
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        visitedStack.push(jcAssign);
        super.visitAssign(jcAssign);
        visitedStack.pop();
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        visitedStack.push(jcAssignOp);
        super.visitAssignop(jcAssignOp);
        visitedStack.pop();
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        visitedStack.push(jcUnary);
        super.visitUnary(jcUnary);
        visitedStack.pop();
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        visitedStack.push(jcBinary);
        super.visitBinary(jcBinary);
        visitedStack.pop();
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        visitedStack.push(jcTypeCast);
        super.visitTypeCast(jcTypeCast);
        visitedStack.pop();
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        visitedStack.push(jcInstanceOf);
        super.visitTypeTest(jcInstanceOf);
        visitedStack.pop();
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        visitedStack.push(jcArrayAccess);
        super.visitIndexed(jcArrayAccess);
        visitedStack.pop();
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        visitedStack.push(jcFieldAccess);
        super.visitSelect(jcFieldAccess);
        visitedStack.pop();
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        visitedStack.push(jcMemberReference);
        super.visitReference(jcMemberReference);
        visitedStack.pop();
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        visitedStack.push(jcIdent);
        super.visitIdent(jcIdent);
        visitedStack.pop();
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        visitedStack.push(jcLiteral);
        super.visitLiteral(jcLiteral);
        visitedStack.pop();
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        visitedStack.push(jcPrimitiveTypeTree);
        super.visitTypeIdent(jcPrimitiveTypeTree);
        visitedStack.pop();
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        visitedStack.push(jcArrayTypeTree);
        super.visitTypeArray(jcArrayTypeTree);
        visitedStack.pop();
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        visitedStack.push(jcTypeApply);
        super.visitTypeApply(jcTypeApply);
        visitedStack.pop();
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        visitedStack.push(jcTypeUnion);
        super.visitTypeUnion(jcTypeUnion);
        visitedStack.pop();
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        visitedStack.push(jcTypeIntersection);
        super.visitTypeIntersection(jcTypeIntersection);
        visitedStack.pop();
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        visitedStack.push(jcTypeParameter);
        super.visitTypeParameter(jcTypeParameter);
        visitedStack.pop();
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        visitedStack.push(jcWildcard);
        super.visitWildcard(jcWildcard);
        visitedStack.pop();
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        visitedStack.push(typeBoundKind);
        super.visitTypeBoundKind(typeBoundKind);
        visitedStack.pop();
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        visitedStack.push(jcErroneous);
        super.visitErroneous(jcErroneous);
        visitedStack.pop();
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        visitedStack.push(letExpr);
        super.visitLetExpr(letExpr);
        visitedStack.pop();
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        visitedStack.push(jcModifiers);
        super.visitModifiers(jcModifiers);
        visitedStack.pop();
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        visitedStack.push(jcAnnotation);
        super.visitAnnotation(jcAnnotation);
        visitedStack.pop();
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        visitedStack.push(jcAnnotatedType);
        super.visitAnnotatedType(jcAnnotatedType);
        visitedStack.pop();
    }

    /**
     * Helper method to insert the given statement into the body of the block enclosing the currently-being-considered
     * node.
     *
     * @param target The node in the enclosing block to place the new statement immediately before.
     * @param trees The new statement to put into the tree.
     */
    protected void insertIntoEnclosingBlock(JCTree target, List<JCStatement> trees) {
        if (trees.isEmpty()) {
            return;
        }
        mHasMadeAChange = true;

        // Statements can be moved to the parent by considering the top of visitedStack.
        JCBlock enclosingBlock = (JCBlock) visitedStack.peek();
        List<JCStatement> enclosingStatements = enclosingBlock.stats;

        // The index of the while loop inside the enclosing scope - we want to move invariants to here.
        int loopIndex = enclosingStatements.indexOf(target);

        for (JCStatement st : trees) {
            // enclosingStatements.add(loopIndex, tree); throws UnsupportedOperationException. *sigh*
            JavacListUtils.insertAtIndex(enclosingStatements, st, loopIndex);
            loopIndex++;
        }

        log.debug("Inserted {} into {} at index {}", trees, enclosingBlock, loopIndex);
    }
}
