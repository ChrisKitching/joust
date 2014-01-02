package joust.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import java.util.LinkedList;

/**
 * A TreeTranslator storing a stack of the nodes between the current node and the root.
 * Allows translators to access the properties of parent nodes of interest.
 *
 * Unfortunately, the resulting code is horribly, horribly, horrible.
 */
public class ParentTrackingTreeTranslator extends TreeTranslator {
    protected LinkedList<JCTree> visitedStack = new LinkedList<>();

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit jcCompilationUnit) {
        visitedStack.push(jcCompilationUnit);
        super.visitTopLevel(jcCompilationUnit);
        visitedStack.pop();
    }

    @Override
    public void visitImport(JCTree.JCImport jcImport) {
        visitedStack.push(jcImport);
        super.visitImport(jcImport);
        visitedStack.pop();
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        visitedStack.push(jcClassDecl);
        super.visitClassDef(jcClassDecl);
        visitedStack.pop();
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        visitedStack.push(jcMethodDecl);
        super.visitMethodDef(jcMethodDecl);
        visitedStack.pop();
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        visitedStack.push(jcVariableDecl);
        super.visitVarDef(jcVariableDecl);
        visitedStack.pop();
    }

    @Override
    public void visitSkip(JCTree.JCSkip jcSkip) {
        visitedStack.push(jcSkip);
        super.visitSkip(jcSkip);
        visitedStack.pop();
    }

    @Override
    public void visitBlock(JCTree.JCBlock jcBlock) {
        visitedStack.push(jcBlock);
        super.visitBlock(jcBlock);
        visitedStack.pop();
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop jcDoWhileLoop) {
        visitedStack.push(jcDoWhileLoop);
        super.visitDoLoop(jcDoWhileLoop);
        visitedStack.pop();
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop jcWhileLoop) {
        visitedStack.push(jcWhileLoop);
        super.visitWhileLoop(jcWhileLoop);
        visitedStack.pop();
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop) {
        visitedStack.push(jcForLoop);
        super.visitForLoop(jcForLoop);
        visitedStack.pop();
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop) {
        visitedStack.push(jcEnhancedForLoop);
        super.visitForeachLoop(jcEnhancedForLoop);
        visitedStack.pop();
    }

    @Override
    public void visitLabelled(JCTree.JCLabeledStatement jcLabeledStatement) {
        visitedStack.push(jcLabeledStatement);
        super.visitLabelled(jcLabeledStatement);
        visitedStack.pop();
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch jcSwitch) {
        visitedStack.push(jcSwitch);
        super.visitSwitch(jcSwitch);
        visitedStack.pop();
    }

    @Override
    public void visitCase(JCTree.JCCase jcCase) {
        visitedStack.push(jcCase);
        super.visitCase(jcCase);
        visitedStack.pop();
    }

    @Override
    public void visitSynchronized(JCTree.JCSynchronized jcSynchronized) {
        visitedStack.push(jcSynchronized);
        super.visitSynchronized(jcSynchronized);
        visitedStack.pop();
    }

    @Override
    public void visitTry(JCTree.JCTry jcTry) {
        visitedStack.push(jcTry);
        super.visitTry(jcTry);
        visitedStack.pop();
    }

    @Override
    public void visitCatch(JCTree.JCCatch jcCatch) {
        visitedStack.push(jcCatch);
        super.visitCatch(jcCatch);
        visitedStack.pop();
    }

    @Override
    public void visitConditional(JCTree.JCConditional jcConditional) {
        visitedStack.push(jcConditional);
        super.visitConditional(jcConditional);
        visitedStack.pop();
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf) {
        visitedStack.push(jcIf);
        super.visitIf(jcIf);
        visitedStack.pop();
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement jcExpressionStatement) {
        visitedStack.push(jcExpressionStatement);
        super.visitExec(jcExpressionStatement);
        visitedStack.pop();
    }

    @Override
    public void visitBreak(JCTree.JCBreak jcBreak) {
        visitedStack.push(jcBreak);
        super.visitBreak(jcBreak);
        visitedStack.pop();
    }

    @Override
    public void visitContinue(JCTree.JCContinue jcContinue) {
        visitedStack.push(jcContinue);
        super.visitContinue(jcContinue);
        visitedStack.pop();
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        visitedStack.push(jcReturn);
        super.visitReturn(jcReturn);
        visitedStack.pop();
    }

    @Override
    public void visitThrow(JCTree.JCThrow jcThrow) {
        visitedStack.push(jcThrow);
        super.visitThrow(jcThrow);
        visitedStack.pop();
    }

    @Override
    public void visitAssert(JCTree.JCAssert jcAssert) {
        visitedStack.push(jcAssert);
        super.visitAssert(jcAssert);
        visitedStack.pop();
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation jcMethodInvocation) {
        visitedStack.push(jcMethodInvocation);
        super.visitApply(jcMethodInvocation);
        visitedStack.pop();
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass jcNewClass) {
        visitedStack.push(jcNewClass);
        super.visitNewClass(jcNewClass);
        visitedStack.pop();
    }

    @Override
    public void visitLambda(JCTree.JCLambda jcLambda) {
        visitedStack.push(jcLambda);
        super.visitLambda(jcLambda);
        visitedStack.pop();
    }

    @Override
    public void visitNewArray(JCTree.JCNewArray jcNewArray) {
        visitedStack.push(jcNewArray);
        super.visitNewArray(jcNewArray);
        visitedStack.pop();
    }

    @Override
    public void visitParens(JCTree.JCParens jcParens) {
        visitedStack.push(jcParens);
        super.visitParens(jcParens);
        visitedStack.pop();
    }

    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        visitedStack.push(jcAssign);
        super.visitAssign(jcAssign);
        visitedStack.pop();
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        visitedStack.push(jcAssignOp);
        super.visitAssignop(jcAssignOp);
        visitedStack.pop();
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary) {
        visitedStack.push(jcUnary);
        super.visitUnary(jcUnary);
        visitedStack.pop();
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary) {
        visitedStack.push(jcBinary);
        super.visitBinary(jcBinary);
        visitedStack.pop();
    }

    @Override
    public void visitTypeCast(JCTree.JCTypeCast jcTypeCast) {
        visitedStack.push(jcTypeCast);
        super.visitTypeCast(jcTypeCast);
        visitedStack.pop();
    }

    @Override
    public void visitTypeTest(JCTree.JCInstanceOf jcInstanceOf) {
        visitedStack.push(jcInstanceOf);
        super.visitTypeTest(jcInstanceOf);
        visitedStack.pop();
    }

    @Override
    public void visitIndexed(JCTree.JCArrayAccess jcArrayAccess) {
        visitedStack.push(jcArrayAccess);
        super.visitIndexed(jcArrayAccess);
        visitedStack.pop();
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess) {
        visitedStack.push(jcFieldAccess);
        super.visitSelect(jcFieldAccess);
        visitedStack.pop();
    }

    @Override
    public void visitReference(JCTree.JCMemberReference jcMemberReference) {
        visitedStack.push(jcMemberReference);
        super.visitReference(jcMemberReference);
        visitedStack.pop();
    }

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        visitedStack.push(jcIdent);
        super.visitIdent(jcIdent);
        visitedStack.pop();
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral jcLiteral) {
        visitedStack.push(jcLiteral);
        super.visitLiteral(jcLiteral);
        visitedStack.pop();
    }

    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        visitedStack.push(jcPrimitiveTypeTree);
        super.visitTypeIdent(jcPrimitiveTypeTree);
        visitedStack.pop();
    }

    @Override
    public void visitTypeArray(JCTree.JCArrayTypeTree jcArrayTypeTree) {
        visitedStack.push(jcArrayTypeTree);
        super.visitTypeArray(jcArrayTypeTree);
        visitedStack.pop();
    }

    @Override
    public void visitTypeApply(JCTree.JCTypeApply jcTypeApply) {
        visitedStack.push(jcTypeApply);
        super.visitTypeApply(jcTypeApply);
        visitedStack.pop();
    }

    @Override
    public void visitTypeUnion(JCTree.JCTypeUnion jcTypeUnion) {
        visitedStack.push(jcTypeUnion);
        super.visitTypeUnion(jcTypeUnion);
        visitedStack.pop();
    }

    @Override
    public void visitTypeIntersection(JCTree.JCTypeIntersection jcTypeIntersection) {
        visitedStack.push(jcTypeIntersection);
        super.visitTypeIntersection(jcTypeIntersection);
        visitedStack.pop();
    }

    @Override
    public void visitTypeParameter(JCTree.JCTypeParameter jcTypeParameter) {
        visitedStack.push(jcTypeParameter);
        super.visitTypeParameter(jcTypeParameter);
        visitedStack.pop();
    }

    @Override
    public void visitWildcard(JCTree.JCWildcard jcWildcard) {
        visitedStack.push(jcWildcard);
        super.visitWildcard(jcWildcard);
        visitedStack.pop();
    }

    @Override
    public void visitTypeBoundKind(JCTree.TypeBoundKind typeBoundKind) {
        visitedStack.push(typeBoundKind);
        super.visitTypeBoundKind(typeBoundKind);
        visitedStack.pop();
    }

    @Override
    public void visitErroneous(JCTree.JCErroneous jcErroneous) {
        visitedStack.push(jcErroneous);
        super.visitErroneous(jcErroneous);
        visitedStack.pop();
    }

    @Override
    public void visitLetExpr(JCTree.LetExpr letExpr) {
        visitedStack.push(letExpr);
        super.visitLetExpr(letExpr);
        visitedStack.pop();
    }

    @Override
    public void visitModifiers(JCTree.JCModifiers jcModifiers) {
        visitedStack.push(jcModifiers);
        super.visitModifiers(jcModifiers);
        visitedStack.pop();
    }

    @Override
    public void visitAnnotation(JCTree.JCAnnotation jcAnnotation) {
        visitedStack.push(jcAnnotation);
        super.visitAnnotation(jcAnnotation);
        visitedStack.pop();
    }

    @Override
    public void visitAnnotatedType(JCTree.JCAnnotatedType jcAnnotatedType) {
        visitedStack.push(jcAnnotatedType);
        super.visitAnnotatedType(jcAnnotatedType);
        visitedStack.pop();
    }
}
