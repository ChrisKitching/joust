package joust.optimisers.visitors;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;

import static com.sun.tools.javac.tree.JCTree.*;

public @Log4j2 class BackwardsFlowVisitor extends DepthFirstTreeVisitor {
    protected HashSet<JCTree> mMarked = new HashSet<>();

    protected void visitBackwards(List<? extends JCTree> aTrees) {
        List<? extends JCTree> trees = aTrees.reverse();
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
            if (l.head != null && !mMarked.contains(l.head)) {
                log.trace("Visit statement: \n{}:{}", l.head, l.head.getClass().getName());
                l.head.accept(this);
            }
        }
    }

    protected void visit(JCTree tree) {
        if (tree != null) {
            if (mMarked.contains(tree)) {
                return;
            }

            log.trace("Visit statement: \n{}:{}", tree, tree.getClass().getName());

            tree.accept(this);
        }
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        if (mMarked.contains(jcBlock)) {
            return;
        }

        visitBackwards(jcBlock.stats);

        mMarked.add(jcBlock);
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop jcDoWhileLoop) {
        if (mMarked.contains(jcDoWhileLoop)) {
            return;
        }

        visit(jcDoWhileLoop.cond);
        visit(jcDoWhileLoop.body);

        mMarked.add(jcDoWhileLoop);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        if (mMarked.contains(jcWhileLoop)) {
            return;
        }

        visit(jcWhileLoop.body);
        visit(jcWhileLoop.cond);

        mMarked.add(jcWhileLoop);
    }

    @Override
    public void visitForLoop(JCForLoop jcForLoop) {
        if (mMarked.contains(jcForLoop)) {
            return;
        }

        visit(jcForLoop.body);
        visitBackwards(jcForLoop.step);
        visit(jcForLoop.cond);
        visitBackwards(jcForLoop.init);

        mMarked.add(jcForLoop);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop jcEnhancedForLoop) {
        if (mMarked.contains(jcEnhancedForLoop)) {
            return;
        }

        visit(jcEnhancedForLoop.body);
        visit(jcEnhancedForLoop.expr);
        visit(jcEnhancedForLoop.var);

        mMarked.add(jcEnhancedForLoop);
    }

    @Override
    public void visitSwitch(JCSwitch jcSwitch) {
        if (mMarked.contains(jcSwitch)) {
            return;
        }

        visitBackwards(jcSwitch.cases);
        visit(jcSwitch.selector);

        mMarked.add(jcSwitch);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        if (mMarked.contains(jcCase)) {
            return;
        }

        visitBackwards(jcCase.stats);
        visit(jcCase.pat);

        mMarked.add(jcCase);
    }

    @Override
    public void visitSynchronized(JCSynchronized jcSynchronized) {
        if (mMarked.contains(jcSynchronized)) {
            return;
        }

        visit(jcSynchronized.body);
        visit(jcSynchronized.lock);

        mMarked.add(jcSynchronized);
    }

    @Override
    public void visitTry(JCTry jcTry) {
        if (mMarked.contains(jcTry)) {
            return;
        }

        visit(jcTry.finalizer);
        visitBackwards(jcTry.catchers);
        visit(jcTry.body);
        visitBackwards(jcTry.resources);

        mMarked.add(jcTry);
    }

    @Override
    public void visitCatch(JCCatch jcCatch) {
        if (mMarked.contains(jcCatch)) {
            return;
        }

        visit(jcCatch.body);
        visit(jcCatch.param);

        mMarked.add(jcCatch);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        if (mMarked.contains(jcConditional)) {
            return;
        }

        visit(jcConditional.falsepart);
        visit(jcConditional.truepart);
        visit(jcConditional.cond);

        mMarked.add(jcConditional);
    }

    @Override
    public void visitIf(JCIf jcIf) {
        if (mMarked.contains(jcIf)) {
            return;
        }

        visit(jcIf.elsepart);
        visit(jcIf.thenpart);
        visit(jcIf.cond);

        mMarked.add(jcIf);
    }

    @Override
    public void visitAssert(JCAssert jcAssert) {
        if (mMarked.contains(jcAssert)) {
            return;
        }

        visit(jcAssert.detail);
        visit(jcAssert.cond);

        mMarked.add(jcAssert);
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        if (mMarked.contains(jcMethodDecl)) {
            return;
        }

        visit(jcMethodDecl.body);
        visit(jcMethodDecl.thrown);
        visit(jcMethodDecl.params);
        visit(jcMethodDecl.recvparam);
        visit(jcMethodDecl.typarams);
        visit(jcMethodDecl.restype);
        visit(jcMethodDecl.mods);

        mMarked.add(jcMethodDecl);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        if (mMarked.contains(jcMethodInvocation)) {
            return;
        }

        visitBackwards(jcMethodInvocation.args);
        visit(jcMethodInvocation.meth);

        mMarked.add(jcMethodInvocation);
    }

    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        if (mMarked.contains(jcNewClass)) {
            return;
        }

        visit(jcNewClass.def);
        visitBackwards(jcNewClass.args);
        visit(jcNewClass.clazz);
        visit(jcNewClass.encl);

        mMarked.add(jcNewClass);
    }

    @Override
    public void visitNewArray(JCNewArray jcNewArray) {
        if (mMarked.contains(jcNewArray)) {
            return;
        }

        visitBackwards(jcNewArray.elems);
        visitBackwards(jcNewArray.dims);
        visit(jcNewArray.elemtype);

        for (List<JCAnnotation> dimAnno : jcNewArray.dimAnnotations) {
            visitBackwards(dimAnno);
        }

        visitBackwards(jcNewArray.annotations);

        mMarked.add(jcNewArray);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        if (mMarked.contains(jcLambda)) {
            return;
        }

        visit(jcLambda.body);
        visitBackwards(jcLambda.params);

        mMarked.add(jcLambda);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        if (mMarked.contains(jcAssign)) {
            return;
        }

        visit(jcAssign.rhs);
        visit(jcAssign.lhs);

        mMarked.add(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        if (mMarked.contains(jcAssignOp)) {
            return;
        }

        visit(jcAssignOp.rhs);
        visit(jcAssignOp.lhs);

        mMarked.add(jcAssignOp);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        if (mMarked.contains(jcBinary)) {
            return;
        }

        visit(jcBinary.rhs);
        visit(jcBinary.lhs);

        mMarked.add(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        if (mMarked.contains(jcTypeCast)) {
            return;
        }

        visit(jcTypeCast.expr);
        visit(jcTypeCast.clazz);

        mMarked.add(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        if (mMarked.contains(jcInstanceOf)) {
            return;
        }

        visit(jcInstanceOf.clazz);
        visit(jcInstanceOf.expr);

        mMarked.add(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        if (mMarked.contains(jcArrayAccess)) {
            return;
        }

        visit(jcArrayAccess.index);
        visit(jcArrayAccess.indexed);

        mMarked.add(jcArrayAccess);
    }

    @Override
    public void visitTypeApply(JCTypeApply jcTypeApply) {
        if (mMarked.contains(jcTypeApply)) {
            return;
        }

        visitBackwards(jcTypeApply.arguments);
        visit(jcTypeApply.clazz);

        mMarked.add(jcTypeApply);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter jcTypeParameter) {
        if (mMarked.contains(jcTypeParameter)) {
            return;
        }

        visitBackwards(jcTypeParameter.annotations);
        visitBackwards(jcTypeParameter.bounds);

        mMarked.add(jcTypeParameter);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        if (mMarked.contains(jcAnnotation)) {
            return;
        }

        visitBackwards(jcAnnotation.args);
        visit(jcAnnotation.annotationType);

        mMarked.add(jcAnnotation);
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType jcAnnotatedType) {
        if (mMarked.contains(jcAnnotatedType)) {
            return;
        }

        visit(jcAnnotatedType.underlyingType);
        visitBackwards(jcAnnotatedType.annotations);

        mMarked.add(jcAnnotatedType);
    }

    @Override
    public void visitLetExpr(LetExpr letExpr) {
        if (mMarked.contains(letExpr)) {
            return;
        }

        visit(letExpr.expr);
        visitBackwards(letExpr.defs);

        mMarked.add(letExpr);
    }

    @Override
    public void visitTree(JCTree jcTree) {
        log.error("JOUST visiting unknown tree node:\n{}\nThis is unlikely to end well. Continuing anyway.", jcTree);
        mMarked.add(jcTree);
    }
}
