package joust.analysers;

import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class BackwardsFlowVisitor extends AJCTreeVisitor {
    protected void visitBackwards(List<? extends AJCTree> aTrees) {
        List<? extends AJCTree> trees = aTrees.reverse();
        for (List<? extends AJCTree> l = trees; l.nonEmpty(); l = l.tail) {
            if (l.head != null) {
                visit(l.head);
            }
        }
    }

    @Override
    public void visitBlock(AJCBlock jcBlock) {
        visitBackwards(jcBlock.stats);
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop jcDoWhileLoop) {
        visit(jcDoWhileLoop.cond);
        visit(jcDoWhileLoop.body);
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop jcWhileLoop) {
        visit(jcWhileLoop.body);
        visit(jcWhileLoop.cond);
    }

    @Override
    public void visitForLoop(AJCForLoop jcForLoop) {
        visit(jcForLoop.body);
        visitBackwards(jcForLoop.step);
        visit(jcForLoop.cond);
        visitBackwards(jcForLoop.init);
    }

    @Override
    public void visitSwitch(AJCSwitch jcSwitch) {
        visitBackwards(jcSwitch.cases);
        visit(jcSwitch.selector);
    }

    @Override
    public void visitCase(AJCCase jcCase) {
        visitBackwards(jcCase.stats);
        visit(jcCase.pat);
    }

    @Override
    public void visitSynchronized(AJCSynchronized jcSynchronized) {
        visit(jcSynchronized.body);
        visit(jcSynchronized.lock);
    }

    @Override
    public void visitTry(AJCTry jcTry) {
        visit(jcTry.finalizer);
        visitBackwards(jcTry.catchers);
        visit(jcTry.body);
    }

    @Override
    public void visitCatch(AJCCatch jcCatch) {
        visit(jcCatch.body);
        visit(jcCatch.param);
    }

    @Override
    public void visitConditional(AJCConditional jcConditional) {
        visit(jcConditional.falsepart);
        visit(jcConditional.truepart);
        visit(jcConditional.cond);
    }

    @Override
    public void visitIf(AJCIf jcIf) {
        visit(jcIf.elsepart);
        visit(jcIf.thenpart);
        visit(jcIf.cond);
    }

    @Override
    public void visitMethodDef(AJCMethodDecl jcMethodDecl) {
        visit(jcMethodDecl.body);
        visit(jcMethodDecl.thrown);
        visit(jcMethodDecl.params);
        visit(jcMethodDecl.recvparam);
        visit(jcMethodDecl.restype);
        visit(jcMethodDecl.mods);
    }

    @Override
    public void visitCall(AJCCall jcMethodInvocation) {
        visitBackwards(jcMethodInvocation.args);
        visit(jcMethodInvocation.meth);
    }

    @Override
    public void visitNewClass(AJCNewClass jcNewClass) {
        visit(jcNewClass.def);
        visitBackwards(jcNewClass.args);
        visit(jcNewClass.clazz);
    }

    @Override
    public void visitNewArray(AJCNewArray jcNewArray) {
        visitBackwards(jcNewArray.elems);
        visitBackwards(jcNewArray.dims);
        visit(jcNewArray.elemtype);
        visitBackwards(jcNewArray.annotations);
    }

    @Override
    public void visitAssign(AJCAssign jcAssign) {
        visit(jcAssign.rhs);
        visit(jcAssign.lhs);
    }

    @Override
    public void visitAssignop(AJCAssignOp jcAssignOp) {
        visit(jcAssignOp.rhs);
        visit(jcAssignOp.lhs);
    }

    @Override
    public void visitBinary(AJCBinary jcBinary) {
        visit(jcBinary.rhs);
        visit(jcBinary.lhs);
    }

    @Override
    public void visitTypeCast(AJCTypeCast jcTypeCast) {
        visit(jcTypeCast.expr);
        visit(jcTypeCast.clazz);
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf jcInstanceOf) {
        visit((AJCTree) jcInstanceOf.clazz);
        visit(jcInstanceOf.expr);
    }

    @Override
    public void visitArrayAccess(AJCArrayAccess jcArrayAccess) {
        visit(jcArrayAccess.index);
        visit(jcArrayAccess.indexed);
    }

    @Override
    public void visitLetExpr(AJCLetExpr letExpr) {
        visit(letExpr.expr);
        visitBackwards(letExpr.defs);
    }
}
