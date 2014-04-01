package joust.utils;

import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * Utility class for depth-first traversing a Javac AST and printing the nodes encountered in the order so met.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class JCTreeStructurePrinter extends DepthFirstJCTreeVisitor {
    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        log.info("{}:{}", jcCompilationUnit, jcCompilationUnit.getClass().getCanonicalName());
        super.visitTopLevel(jcCompilationUnit);
    }

    @Override
    public void visitImport(JCImport jcImport) {
        log.info("{}:{}", jcImport, jcImport.getClass().getCanonicalName());
        super.visitImport(jcImport);
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        log.info("{}:{}", jcClassDecl, jcClassDecl.getClass().getCanonicalName());
        super.visitClassDef(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        log.info("{}:{}", jcMethodDecl, jcMethodDecl.getClass().getCanonicalName());
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        log.info("{}:{}", jcVariableDecl, jcVariableDecl.getClass().getCanonicalName());
        super.visitVarDef(jcVariableDecl);
    }

    @Override
    public void visitSkip(JCSkip jcSkip) {
        log.info("{}:{}", jcSkip, jcSkip.getClass().getCanonicalName());
        super.visitSkip(jcSkip);
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        log.info("{}:{}", jcBlock, jcBlock.getClass().getCanonicalName());
        super.visitBlock(jcBlock);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        log.info("{}:{}", jcDoWhileLoop, jcDoWhileLoop.getClass().getCanonicalName());
        super.visitDoLoop(jcDoWhileLoop);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        log.info("{}:{}", jcWhileLoop, jcWhileLoop.getClass().getCanonicalName());
        super.visitWhileLoop(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        log.info("{}:{}", jcForLoop, jcForLoop.getClass().getCanonicalName());
        super.visitForLoop(jcForLoop);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        log.info("{}:{}", jcEnhancedForLoop, jcEnhancedForLoop.getClass().getCanonicalName());
        super.visitForeachLoop(jcEnhancedForLoop);
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        log.info("{}:{}", jcLabeledStatement, jcLabeledStatement.getClass().getCanonicalName());
        super.visitLabelled(jcLabeledStatement);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        log.info("{}:{}", jcSwitch, jcSwitch.getClass().getCanonicalName());
        super.visitSwitch(jcSwitch);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        log.info("{}:{}", jcCase, jcCase.getClass().getCanonicalName());
        super.visitCase(jcCase);
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        log.info("{}:{}", jcSynchronized, jcSynchronized.getClass().getCanonicalName());
        super.visitSynchronized(jcSynchronized);
    }

    @Override
    public void visitTry(JCTry jcTry) {
        log.info("{}:{}", jcTry, jcTry.getClass().getCanonicalName());
        super.visitTry(jcTry);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        log.info("{}:{}", jcCatch, jcCatch.getClass().getCanonicalName());
        super.visitCatch(jcCatch);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        log.info("{}:{}", jcConditional, jcConditional.getClass().getCanonicalName());
        super.visitConditional(jcConditional);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        log.info("{}:{}", jcIf, jcIf.getClass().getCanonicalName());
        super.visitIf(jcIf);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        log.info("{}:{}", jcExpressionStatement, jcExpressionStatement.getClass().getCanonicalName());
        super.visitExec(jcExpressionStatement);
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        log.info("{}:{}", jcBreak, jcBreak.getClass().getCanonicalName());
        super.visitBreak(jcBreak);
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        log.info("{}:{}", jcContinue, jcContinue.getClass().getCanonicalName());
        super.visitContinue(jcContinue);
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        log.info("{}:{}", jcReturn, jcReturn.getClass().getCanonicalName());
        super.visitReturn(jcReturn);
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        log.info("{}:{}", jcThrow, jcThrow.getClass().getCanonicalName());
        super.visitThrow(jcThrow);
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        log.info("{}:{}", jcAssert, jcAssert.getClass().getCanonicalName());
        super.visitAssert(jcAssert);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        log.info("{}:{}", jcMethodInvocation, jcMethodInvocation.getClass().getCanonicalName());
        super.visitApply(jcMethodInvocation);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        log.info("{}:{}", jcNewClass, jcNewClass.getClass().getCanonicalName());
        super.visitNewClass(jcNewClass);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        log.info("{}:{}", jcNewArray, jcNewArray.getClass().getCanonicalName());
        super.visitNewArray(jcNewArray);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        log.info("{}:{}", jcLambda, jcLambda.getClass().getCanonicalName());
        super.visitLambda(jcLambda);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        log.info("{}:{}", jcParens, jcParens.getClass().getCanonicalName());
        super.visitParens(jcParens);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        log.info("{}:{}", jcAssign, jcAssign.getClass().getCanonicalName());
        super.visitAssign(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        log.info("{}:{}", jcAssignOp, jcAssignOp.getClass().getCanonicalName());
        super.visitAssignop(jcAssignOp);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        log.info("{}:{}", jcUnary, jcUnary.getClass().getCanonicalName());
        super.visitUnary(jcUnary);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        log.info("{}:{}", jcBinary, jcBinary.getClass().getCanonicalName());
        super.visitBinary(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        log.info("{}:{}", jcTypeCast, jcTypeCast.getClass().getCanonicalName());
        super.visitTypeCast(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        log.info("{}:{}", jcInstanceOf, jcInstanceOf.getClass().getCanonicalName());
        super.visitTypeTest(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        log.info("{}:{}", jcArrayAccess, jcArrayAccess.getClass().getCanonicalName());
        super.visitIndexed(jcArrayAccess);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        log.info("{}:{}", jcFieldAccess, jcFieldAccess.getClass().getCanonicalName());
        log.debug("{}:{}", jcFieldAccess.selected, jcFieldAccess.selected.getClass().getCanonicalName());
        log.debug(jcFieldAccess.sym);
        log.debug(jcFieldAccess.name);
        super.visitSelect(jcFieldAccess);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        log.info("{}:{}", jcMemberReference, jcMemberReference.getClass().getCanonicalName());
        super.visitReference(jcMemberReference);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        log.info("{}:{}", jcIdent, jcIdent.getClass().getCanonicalName());
        super.visitIdent(jcIdent);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        log.info("{}:{}", jcLiteral, jcLiteral.getClass().getCanonicalName());
        super.visitLiteral(jcLiteral);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        log.info("{}:{}", jcPrimitiveTypeTree, jcPrimitiveTypeTree.getClass().getCanonicalName());
        super.visitTypeIdent(jcPrimitiveTypeTree);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        log.info("{}:{}", jcArrayTypeTree, jcArrayTypeTree.getClass().getCanonicalName());
        super.visitTypeArray(jcArrayTypeTree);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        log.info("{}:{}", jcTypeApply, jcTypeApply.getClass().getCanonicalName());
        super.visitTypeApply(jcTypeApply);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        log.info("{}:{}", jcTypeUnion, jcTypeUnion.getClass().getCanonicalName());
        super.visitTypeUnion(jcTypeUnion);
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        log.info("{}:{}", jcTypeIntersection, jcTypeIntersection.getClass().getCanonicalName());
        super.visitTypeIntersection(jcTypeIntersection);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        log.info("{}:{}", jcTypeParameter, jcTypeParameter.getClass().getCanonicalName());
        super.visitTypeParameter(jcTypeParameter);
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        log.info("{}:{}", jcWildcard, jcWildcard.getClass().getCanonicalName());
        super.visitWildcard(jcWildcard);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        log.info("{}:{}", typeBoundKind, typeBoundKind.getClass().getCanonicalName());
        super.visitTypeBoundKind(typeBoundKind);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        log.info("{}:{}", jcAnnotation, jcAnnotation.getClass().getCanonicalName());
        super.visitAnnotation(jcAnnotation);
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        log.info("{}:{}", jcModifiers, jcModifiers.getClass().getCanonicalName());
        super.visitModifiers(jcModifiers);
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        log.info("{}:{}", jcAnnotatedType, jcAnnotatedType.getClass().getCanonicalName());
        super.visitAnnotatedType(jcAnnotatedType);
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        log.info("{}:{}", jcErroneous, jcErroneous.getClass().getCanonicalName());
        super.visitErroneous(jcErroneous);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        log.info("{}:{}", letExpr, letExpr.getClass().getCanonicalName());
        super.visitLetExpr(letExpr);
    }
}
