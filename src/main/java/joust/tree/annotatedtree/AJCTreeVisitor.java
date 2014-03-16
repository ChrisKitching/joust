package joust.tree.annotatedtree;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * Interface for tree visitors.
 */
public interface AJCTreeVisitor {
    public void visitImport(AJCImport that);           
    public void visitClassDef(AJCClassDecl that);
    public void visitMethodDef(AJCMethodDecl that);
    public void visitVariableDecl(AJCVariableDecl that);
    public void visitEmptyExpression(AJCEmptyExpression that);
    public void visitSkip(AJCSkip that);
    public void visitBlock(AJCBlock that);
    public void visitDoWhileLoop(AJCDoWhileLoop that);
    public void visitWhileLoop(AJCWhileLoop that);
    public void visitForLoop(AJCForLoop that);
    public void visitForeachLoop(AJCForEachLoop that);
    public void visitLabelledStatement(AJCLabeledStatement that);
    public void visitSwitch(AJCSwitch that);
    public void visitCase(AJCCase that);
    public void visitSynchronized(AJCSynchronized that);
    public void visitTry(AJCTry that);
    public void visitCatch(AJCCatch that);
    public void visitConditional(AJCConditional that);
    public void visitIf(AJCIf that);
    public void visitExpressionStatement(AJCExpressionStatement that);
    public void visitBreak(AJCBreak that);
    public void visitContinue(AJCContinue that);
    public void visitReturn(AJCReturn that);
    public void visitThrow(AJCThrow that);
    public void visitAssert(AJCAssert that);
    public void visitCall(AJCCall that);
    public void visitNewClass(AJCNewClass that);
    public void visitNewArray(AJCNewArray that);
    public void visitLambda(AJCLambda that);
    public void visitAssign(AJCAssign that);
    public void visitAssignop(AJCAssignOp that);
    public void visitUnary(AJCUnary that);
    public void visitUnaryAsg(AJCUnaryAsg that);
    public void visitBinary(AJCBinary that);
    public void visitTypeCast(AJCTypeCast that);
    public void visitInstanceOf(AJCInstanceOf that);
    public void visitArrayAccess(AJCArrayAccess that);
    public void visitFieldAccess(AJCFieldAccess that);
    public void visitMemberReference(AJCMemberReference that);
    public void visitIdent(AJCIdent that);
    public void visitLiteral(AJCLiteral that);
    public void visitPrimitiveType(AJCPrimitiveTypeTree that);
    public void visitArrayType(AJCArrayTypeTree that);
    public void visitTypeApply(AJCTypeApply that);
    public void visitTypeUnion(AJCTypeUnion that);
    public void visitTypeIntersection(AJCTypeIntersection that);
    public void visitTypeParameter(AJCTypeParameter that);
    public void visitWildcard(AJCWildcard that);
    public void visitTypeBoundKind(AJCTypeBoundKind that);
    public void visitAnnotation(AJCAnnotation that);
    public void visitModifiers(AJCModifiers that);
    public void visitAnnotatedType(AJCAnnotatedType that);
    public void visitErroneous(AJCErroneous that);
    public void visitLetExpr(AJCLetExpr that);
}
