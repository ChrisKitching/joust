package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A tree translator that keeps a to-do list of nodes to replace with other nodes.
 * Useful for situations where analysis shows that some distant node needs replacing in a certain way, but for
 * which we do not, at that time, have a reference to the enclosing block for.
 */
public @Log4j2
abstract class TODOTranslator extends ParentTrackingTreeTranslator {
    // Trees to swap for other trees.
    protected HashMap<JCTree, JCTree> substitutions = new HashMap<>();

    private void considerSwap(JCTree tree) {
        JCTree swap = substitutions.get(tree);
        if (swap != null) {
            mHasMadeAChange = true;
            log.info("TODO swapping {} for {}", tree, swap);
            result = swap;
            substitutions.remove(tree);
        }
    }

    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        super.visitTopLevel(jcCompilationUnit);
        considerSwap(jcCompilationUnit);
    }

    @Override
    public void visitImport(JCImport jcImport) {
        super.visitImport(jcImport);
        considerSwap(jcImport);
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        super.visitClassDef(jcClassDecl);
        considerSwap(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        super.visitMethodDef(jcMethodDecl);
        considerSwap(jcMethodDecl);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);
        considerSwap(jcVariableDecl);
    }

    @Override
    public void visitSkip(JCSkip jcSkip) {
        super.visitSkip(jcSkip);
        considerSwap(jcSkip);
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        super.visitBlock(jcBlock);
        considerSwap(jcBlock);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        super.visitDoLoop(jcDoWhileLoop);
        considerSwap(jcDoWhileLoop);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        super.visitWhileLoop(jcWhileLoop);
        considerSwap(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        super.visitForLoop(jcForLoop);
        considerSwap(jcForLoop);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        super.visitForeachLoop(jcEnhancedForLoop);
        considerSwap(jcEnhancedForLoop);
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        super.visitLabelled(jcLabeledStatement);
        considerSwap(jcLabeledStatement);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        super.visitSwitch(jcSwitch);
        considerSwap(jcSwitch);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        super.visitCase(jcCase);
        considerSwap(jcCase);
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        super.visitSynchronized(jcSynchronized);
        considerSwap(jcSynchronized);
    }

    @Override
    public void visitTry(JCTry jcTry) {
        super.visitTry(jcTry);
        considerSwap(jcTry);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        super.visitCatch(jcCatch);
        considerSwap(jcCatch);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        super.visitConditional(jcConditional);
        considerSwap(jcConditional);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        super.visitIf(jcIf);
        considerSwap(jcIf);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        super.visitExec(jcExpressionStatement);
        considerSwap(jcExpressionStatement);
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        super.visitBreak(jcBreak);
        considerSwap(jcBreak);
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        super.visitContinue(jcContinue);
        considerSwap(jcContinue);
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        super.visitReturn(jcReturn);
        considerSwap(jcReturn);
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        super.visitThrow(jcThrow);
        considerSwap(jcThrow);
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        super.visitAssert(jcAssert);
        considerSwap(jcAssert);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        super.visitApply(jcMethodInvocation);
        considerSwap(jcMethodInvocation);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        super.visitNewClass(jcNewClass);
        considerSwap(jcNewClass);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        super.visitLambda(jcLambda);
        considerSwap(jcLambda);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        super.visitNewArray(jcNewArray);
        considerSwap(jcNewArray);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        super.visitParens(jcParens);
        considerSwap(jcParens);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        super.visitAssign(jcAssign);
        considerSwap(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);
        considerSwap(jcAssignOp);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        super.visitUnary(jcUnary);
        considerSwap(jcUnary);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        super.visitBinary(jcBinary);
        considerSwap(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        super.visitTypeCast(jcTypeCast);
        considerSwap(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        super.visitTypeTest(jcInstanceOf);
        considerSwap(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        super.visitIndexed(jcArrayAccess);
        considerSwap(jcArrayAccess);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        super.visitSelect(jcFieldAccess);
        considerSwap(jcFieldAccess);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        super.visitReference(jcMemberReference);
        considerSwap(jcMemberReference);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        super.visitIdent(jcIdent);
        considerSwap(jcIdent);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        super.visitLiteral(jcLiteral);
        considerSwap(jcLiteral);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        super.visitTypeIdent(jcPrimitiveTypeTree);
        considerSwap(jcPrimitiveTypeTree);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        super.visitTypeArray(jcArrayTypeTree);
        considerSwap(jcArrayTypeTree);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        super.visitTypeApply(jcTypeApply);
        considerSwap(jcTypeApply);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        super.visitTypeUnion(jcTypeUnion);
        considerSwap(jcTypeUnion);
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        super.visitTypeIntersection(jcTypeIntersection);
        considerSwap(jcTypeIntersection);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        super.visitTypeParameter(jcTypeParameter);
        considerSwap(jcTypeParameter);
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        super.visitWildcard(jcWildcard);
        considerSwap(jcWildcard);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        super.visitTypeBoundKind(typeBoundKind);
        considerSwap(typeBoundKind);
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        super.visitErroneous(jcErroneous);
        considerSwap(jcErroneous);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        super.visitLetExpr(letExpr);
        considerSwap(letExpr);
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        super.visitModifiers(jcModifiers);
        considerSwap(jcModifiers);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
        considerSwap(jcAnnotation);
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        super.visitAnnotatedType(jcAnnotatedType);
        considerSwap(jcAnnotatedType);
    }
}
