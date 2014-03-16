package joust.tree.annotatedtree;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * Interface for tree visitors.
 */
public interface AJCTreeVisitor {
    void visitClassDef(AJCClassDecl that);
    void visitMethodDef(AJCMethodDecl that);
    void visitVariableDecl(AJCVariableDecl that);
    void visitEmptyExpression(AJCEmptyExpression that);
    void visitSkip(AJCSkip that);
    void visitBlock(AJCBlock that);
    void visitDoWhileLoop(AJCDoWhileLoop that);
    void visitWhileLoop(AJCWhileLoop that);
    void visitForLoop(AJCForLoop that);
    void visitLabelledStatement(AJCLabeledStatement that);
    void visitSwitch(AJCSwitch that);
    void visitCase(AJCCase that);
    void visitSynchronized(AJCSynchronized that);
    void visitTry(AJCTry that);
    void visitCatch(AJCCatch that);
    void visitConditional(AJCConditional that);
    void visitIf(AJCIf that);
    void visitExpressionStatement(AJCExpressionStatement that);
    void visitBreak(AJCBreak that);
    void visitContinue(AJCContinue that);
    void visitReturn(AJCReturn that);
    void visitThrow(AJCThrow that);
    void visitCall(AJCCall that);
    void visitNewClass(AJCNewClass that);
    void visitNewArray(AJCNewArray that);
    void visitAssign(AJCAssign that);
    void visitAssignop(AJCAssignOp that);
    void visitUnary(AJCUnary that);
    void visitUnaryAsg(AJCUnaryAsg that);
    void visitBinary(AJCBinary that);
    void visitTypeCast(AJCTypeCast that);
    void visitInstanceOf(AJCInstanceOf that);
    void visitArrayAccess(AJCArrayAccess that);
    void visitFieldAccess(AJCFieldAccess that);
    void visitIdent(AJCIdent that);
    void visitLiteral(AJCLiteral that);
    void visitPrimitiveType(AJCPrimitiveTypeTree that);
    void visitArrayType(AJCArrayTypeTree that);
    void visitTypeUnion(AJCTypeUnion that);
    void visitAnnotation(AJCAnnotation that);
    void visitModifiers(AJCModifiers that);
    void visitAnnotatedType(AJCAnnotatedType that);
    void visitErroneous(AJCErroneous that);
    void visitLetExpr(AJCLetExpr that);
}
