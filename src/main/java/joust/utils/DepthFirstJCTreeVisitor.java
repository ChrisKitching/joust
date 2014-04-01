package joust.utils;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashSet;
import java.util.logging.Logger;

/**
 * A Visitor that visits all children of each node before processing the current node. Useful for
 * creating tree analysers.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class DepthFirstJCTreeVisitor extends Visitor {
    protected HashSet<JCTree> mMarked = new HashSet<>();

    protected void visit(List<? extends JCTree> trees) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
            if (l.head != null && !mMarked.contains(l.head)) {
                l.head.accept(this);
            }
        }
    }

    protected void visit(JCTree tree) {
        if (tree != null) {
            if (mMarked.contains(tree)) {
                return;
            }

            tree.accept(this);
        }
    }

    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        if (jcCompilationUnit == null || mMarked.contains(jcCompilationUnit)) {
            return;
        }

        visit(jcCompilationUnit.pid);
        visit(jcCompilationUnit.defs);

        mMarked.add(jcCompilationUnit);
    }

    @Override
    public void visitImport(JCImport jcImport) {
        if (mMarked.contains(jcImport)) {
            return;
        }

        visit(jcImport.qualid);
        mMarked.add(jcImport);
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        if (mMarked.contains(jcClassDecl)) {
            return;
        }

        visit(jcClassDecl.mods);
        visit(jcClassDecl.typarams);
        visit(jcClassDecl.extending);
        visit(jcClassDecl.implementing);
        visit(jcClassDecl.defs);

        mMarked.add(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        if (mMarked.contains(jcMethodDecl)) {
            return;
        }

        visit(jcMethodDecl.mods);
        visit(jcMethodDecl.restype);
        visit(jcMethodDecl.typarams);
        visit(jcMethodDecl.recvparam);
        visit(jcMethodDecl.params);
        visit(jcMethodDecl.thrown);
        visit(jcMethodDecl.body);

        mMarked.add(jcMethodDecl);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        if (mMarked.contains(jcVariableDecl)) {
            return;
        }

        visit(jcVariableDecl.mods);
        visit(jcVariableDecl.nameexpr);
        visit(jcVariableDecl.vartype);
        visit(jcVariableDecl.init);

        mMarked.add(jcVariableDecl);
    }

    @Override
    public void visitSkip(JCSkip jcSkip) {
        if (mMarked.contains(jcSkip)) {
            return;
        }

        mMarked.add(jcSkip);
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        if (mMarked.contains(jcBlock)) {
            return;
        }

        visit(jcBlock.stats);

        mMarked.add(jcBlock);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        if (mMarked.contains(jcDoWhileLoop)) {
            return;
        }

        visit(jcDoWhileLoop.body);
        visit(jcDoWhileLoop.cond);

        mMarked.add(jcDoWhileLoop);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        if (mMarked.contains(jcWhileLoop)) {
            return;
        }

        visit(jcWhileLoop.cond);
        visit(jcWhileLoop.body);

        mMarked.add(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        if (mMarked.contains(jcForLoop)) {
            return;
        }

        visit(jcForLoop.init);
        visit(jcForLoop.step);
        visit(jcForLoop.cond);
        visit(jcForLoop.body);

        mMarked.add(jcForLoop);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        if (mMarked.contains(jcEnhancedForLoop)) {
            return;
        }

        visit(jcEnhancedForLoop.var);
        visit(jcEnhancedForLoop.expr);
        visit(jcEnhancedForLoop.body);

        mMarked.add(jcEnhancedForLoop);
    }

    @Override
    public void visitLabelled(JCLabeledStatement jcLabeledStatement) {
        if (mMarked.contains(jcLabeledStatement)) {
            return;
        }

        visit(jcLabeledStatement.body);

        mMarked.add(jcLabeledStatement);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        if (mMarked.contains(jcSwitch)) {
            return;
        }

        visit(jcSwitch.selector);
        visit(jcSwitch.cases);

        mMarked.add(jcSwitch);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        if (mMarked.contains(jcCase)) {
            return;
        }

        visit(jcCase.pat);
        visit(jcCase.stats);

        mMarked.add(jcCase);
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        if (mMarked.contains(jcSynchronized)) {
            return;
        }

        visit(jcSynchronized.lock);
        visit(jcSynchronized.body);

        mMarked.add(jcSynchronized);
    }

    @Override
    public void visitTry(JCTry jcTry) {
        if (mMarked.contains(jcTry)) {
            return;
        }

        visit(jcTry.resources);
        visit(jcTry.body);
        visit(jcTry.catchers);
        visit(jcTry.finalizer);

        mMarked.add(jcTry);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        if (mMarked.contains(jcCatch)) {
            return;
        }

        visit(jcCatch.param);
        visit(jcCatch.body);

        mMarked.add(jcCatch);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        if (mMarked.contains(jcConditional)) {
            return;
        }

        visit(jcConditional.cond);
        visit(jcConditional.truepart);
        visit(jcConditional.falsepart);

        mMarked.add(jcConditional);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        if (mMarked.contains(jcIf)) {
            return;
        }

        visit(jcIf.cond);
        visit(jcIf.thenpart);
        visit(jcIf.elsepart);

        mMarked.add(jcIf);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        if (mMarked.contains(jcExpressionStatement)) {
            return;
        }

        visit(jcExpressionStatement.expr);

        mMarked.add(jcExpressionStatement);
    }

    @Override
    public void visitBreak(JCBreak jcBreak) {
        if (mMarked.contains(jcBreak)) {
            return;
        }

        mMarked.add(jcBreak);
    }

    @Override
    public void visitContinue(JCContinue jcContinue) {
        if (mMarked.contains(jcContinue)) {
            return;
        }

        mMarked.add(jcContinue);
    }

    @Override
    public void visitReturn(JCReturn jcReturn) {
        if (mMarked.contains(jcReturn)) {
            return;
        }

        visit(jcReturn.expr);

        mMarked.add(jcReturn);
    }

    @Override
    public void visitThrow(JCThrow jcThrow) {
        if (mMarked.contains(jcThrow)) {
            return;
        }

        visit(jcThrow.expr);

        mMarked.add(jcThrow);
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        if (mMarked.contains(jcAssert)) {
            return;
        }

        visit(jcAssert.cond);
        visit(jcAssert.detail);

        mMarked.add(jcAssert);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        if (mMarked.contains(jcMethodInvocation)) {
            return;
        }

        visit(jcMethodInvocation.meth);
        visit(jcMethodInvocation.args);

        mMarked.add(jcMethodInvocation);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        if (mMarked.contains(jcNewClass)) {
            return;
        }

        visit(jcNewClass.encl);
        visit(jcNewClass.clazz);
        visit(jcNewClass.args);
        visit(jcNewClass.def);

        mMarked.add(jcNewClass);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        if (mMarked.contains(jcNewArray)) {
            return;
        }

        visit(jcNewArray.annotations);

        for (List<JCAnnotation> dimAnno : jcNewArray.dimAnnotations) {
            visit(dimAnno);
        }

        visit(jcNewArray.elemtype);
        visit(jcNewArray.dims);
        visit(jcNewArray.elems);

        mMarked.add(jcNewArray);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        if (mMarked.contains(jcLambda)) {
            return;
        }

        visit(jcLambda.params);
        visit(jcLambda.body);

        mMarked.add(jcLambda);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        if (mMarked.contains(jcParens)) {
            return;
        }

        visit(jcParens.expr);

        mMarked.add(jcParens);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        if (mMarked.contains(jcAssign)) {
            return;
        }

        visit(jcAssign.lhs);
        visit(jcAssign.rhs);

        mMarked.add(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        if (mMarked.contains(jcAssignOp)) {
            return;
        }

        visit(jcAssignOp.lhs);
        visit(jcAssignOp.rhs);

        mMarked.add(jcAssignOp);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        if (mMarked.contains(jcUnary)) {
            return;
        }

        visit(jcUnary.arg);

        mMarked.add(jcUnary);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        if (mMarked.contains(jcBinary)) {
            return;
        }

        visit(jcBinary.lhs);
        visit(jcBinary.rhs);

        mMarked.add(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        if (mMarked.contains(jcTypeCast)) {
            return;
        }

        visit(jcTypeCast.clazz);
        visit(jcTypeCast.expr);

        mMarked.add(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        if (mMarked.contains(jcInstanceOf)) {
            return;
        }

        visit(jcInstanceOf.expr);
        visit(jcInstanceOf.clazz);

        mMarked.add(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        if (mMarked.contains(jcArrayAccess)) {
            return;
        }

        visit(jcArrayAccess.indexed);
        visit(jcArrayAccess.index);

        mMarked.add(jcArrayAccess);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        if (mMarked.contains(jcFieldAccess)) {
            return;
        }

        visit(jcFieldAccess.selected);

        mMarked.add(jcFieldAccess);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        if (mMarked.contains(jcMemberReference)) {
            return;
        }

        visit(jcMemberReference.expr);

        mMarked.add(jcMemberReference);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        if (mMarked.contains(jcIdent)) {
            return;
        }

        mMarked.add(jcIdent);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        if (mMarked.contains(jcLiteral)) {
            return;
        }

        mMarked.add(jcLiteral);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree jcPrimitiveTypeTree) {
        if (mMarked.contains(jcPrimitiveTypeTree)) {
            return;
        }

        mMarked.add(jcPrimitiveTypeTree);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree jcArrayTypeTree) {
        if (mMarked.contains(jcArrayTypeTree)) {
            return;
        }

        visit(jcArrayTypeTree.elemtype);

        mMarked.add(jcArrayTypeTree);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        if (mMarked.contains(jcTypeApply)) {
            return;
        }

        visit(jcTypeApply.clazz);
        visit(jcTypeApply.arguments);

        mMarked.add(jcTypeApply);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion jcTypeUnion) {
        if (mMarked.contains(jcTypeUnion)) {
            return;
        }

        visit(jcTypeUnion.alternatives);

        mMarked.add(jcTypeUnion);
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection jcTypeIntersection) {
        if (mMarked.contains(jcTypeIntersection)) {
            return;
        }

        visit(jcTypeIntersection.bounds);

        mMarked.add(jcTypeIntersection);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        if (mMarked.contains(jcTypeParameter)) {
            return;
        }

        visit(jcTypeParameter.annotations);
        visit(jcTypeParameter.bounds);

        mMarked.add(jcTypeParameter);
    }

    @Override
    public void visitWildcard(JCWildcard jcWildcard) {
        if (mMarked.contains(jcWildcard)) {
            return;
        }

        visit(jcWildcard.kind);
        visit(jcWildcard.inner);

        mMarked.add(jcWildcard);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind typeBoundKind) {
        if (mMarked.contains(typeBoundKind)) {
            return;
        }

        mMarked.add(typeBoundKind);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        if (mMarked.contains(jcAnnotation)) {
            return;
        }

        visit(jcAnnotation.annotationType);
        visit(jcAnnotation.args);

        mMarked.add(jcAnnotation);
    }

    @Override
    public void visitModifiers(JCModifiers jcModifiers) {
        if (mMarked.contains(jcModifiers)) {
            return;
        }

        visit(jcModifiers.annotations);

        mMarked.add(jcModifiers);
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        if (mMarked.contains(jcAnnotatedType)) {
            return;
        }

        visit(jcAnnotatedType.annotations);
        visit(jcAnnotatedType.underlyingType);

        mMarked.add(jcAnnotatedType);
    }

    @Override
    public void visitErroneous(JCErroneous jcErroneous) {
        if (mMarked.contains(jcErroneous)) {
            return;
        }

        mMarked.add(jcErroneous);

        log.warn("JOUST visiting erroneous node:\n{}\nThis is unlikely to end well. Continuing anyway.", jcErroneous);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        if (mMarked.contains(letExpr)) {
            return;
        }

        visit(letExpr.defs);
        visit(letExpr.expr);

        mMarked.add(letExpr);
    }

    @Override
    public void visitTree(JCTree jcTree) {
        log.error("JOUST visiting unknown tree node:\n{}\nThis is unlikely to end well. Continuing anyway.", jcTree);
        mMarked.add(jcTree);
    }
}

