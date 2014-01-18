package joust.optimisers.avail;

import com.esotericsoftware.minlog.Log;
import com.sun.tools.javac.tree.JCTree;
import joust.treeinfo.TreeInfoManager;
import joust.utils.DepthFirstTreeVisitor;
import lombok.extern.log4j.Log4j2;


import java.util.HashSet;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Perform available expression analysis on methods.
 */
public @Log4j2 class Avail extends DepthFirstTreeVisitor {
    // The symbol of the method currently being processed.
    MethodSymbol enclosingMethod;

    // The current local variable scope.
    AvailScope currentScope;

    // Flag used to indicate if the next block visited should spawn a new scope. Used to handle
    // spontaneous blocks.
    private boolean nextBlockIsNewScope = true;

    private void enterScope() {
        // Create a new local variable scope contained within the current scope and switch to it.
        currentScope = new AvailScope(currentScope, enclosingMethod);
    }

    private void leaveScope() {
        currentScope = currentScope.leave();
    }

    // Store a copy of the current available expression set in the TreeInfo structure for this node.
    private void markAvailableExpressions(JCTree tree) {
        TreeInfoManager.registerAvailables(tree, new HashSet<>(currentScope.availableExpressions));
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        log.debug("Visitng method: "+jcMethodDecl);
        enclosingMethod = jcMethodDecl.sym;
        currentScope = new AvailScope(jcMethodDecl.sym);

        // TODO: Something something nested classes.
        // TODO: Possibly need to prepopulate class scope at this point to avoid collisions...
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        markAvailableExpressions(jcVariableDecl);

        // Enter the symbol.
        currentScope.enter(jcVariableDecl.sym);

        super.visitVarDef(jcVariableDecl);

        // Associate the concrete symbol with the PAE for the initialiser, if any exists.
        JCTree init = jcVariableDecl.getInitializer();
        if (init != null) {
            currentScope.registerSymbolWithExpression(jcVariableDecl.sym, init);
        }
    }

    @Override
    public void visitBlock(JCBlock jcBlock) {
        boolean blockWasNewScope = nextBlockIsNewScope;
        if (nextBlockIsNewScope) {
            log.info("Entering block scope: {}", jcBlock);
            enterScope();
        } else {
            nextBlockIsNewScope = true;
        }

        super.visitBlock(jcBlock);

        if (blockWasNewScope) {
            log.info("Exiting block scope: {}", jcBlock);
            leaveScope();
        }
    }

    @Override
    public void visitWhileLoop(JCWhileLoop jcWhileLoop) {
        log.debug("Entering visitWhileLoop with scope:\n{}", currentScope);

        super.visitWhileLoop(jcWhileLoop);
        log.debug("Exiting visitWhileLoop with scope:\n{}", currentScope);
    }

    @Override
    public void visitCase(JCCase jcCase) {
        // Although each case is in the same scope as the other cases, the "Scope" we're using here
        // is a crazy munging of lexical scope and flow analysis. This is safe. Honest.
        enterScope();
        super.visitCase(jcCase);
        leaveScope();
    }

    // TODO: Something something PolyExpression.
    @Override
    public void visitConditional(JCConditional jcConditional) {
        markAvailableExpressions(jcConditional);
        super.visitConditional(jcConditional);
    }

    @Override
    public void visitExec(JCExpressionStatement jcExpressionStatement) {
        markAvailableExpressions(jcExpressionStatement);
        super.visitExec(jcExpressionStatement);
    }

    @Override
    public void visitParens(JCParens jcParens) {
        markAvailableExpressions(jcParens);
        super.visitParens(jcParens);
        currentScope.enterExpression(jcParens);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        Log.debug("Visit assign!");

        if (mMarked.contains(jcAssign)) {
            return;
        }

        markAvailableExpressions(jcAssign);
        visit(jcAssign.rhs);
        currentScope.enterExpression(jcAssign);

        mMarked.add(jcAssign);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        markAvailableExpressions(jcAssignOp);
        super.visitAssignop(jcAssignOp);
        currentScope.enterExpression(jcAssignOp);
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        markAvailableExpressions(jcUnary);
        super.visitUnary(jcUnary);
        currentScope.enterExpression(jcUnary);
    }

    @Override
    public void visitBinary(JCBinary jcBinary) {
        markAvailableExpressions(jcBinary);
        super.visitBinary(jcBinary);
        currentScope.enterExpression(jcBinary);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        markAvailableExpressions(jcTypeCast);
        super.visitTypeCast(jcTypeCast);
        currentScope.enterExpression(jcTypeCast);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        markAvailableExpressions(jcInstanceOf);
        super.visitTypeTest(jcInstanceOf);
        currentScope.enterExpression(jcInstanceOf);
    }

    @Override
    public void visitIndexed(JCArrayAccess jcArrayAccess) {
        markAvailableExpressions(jcArrayAccess);
        super.visitIndexed(jcArrayAccess);
        currentScope.enterExpression(jcArrayAccess);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        markAvailableExpressions(jcFieldAccess);
        super.visitSelect(jcFieldAccess);
    }

    @Override
    public void visitReference(JCMemberReference jcMemberReference) {
        markAvailableExpressions(jcMemberReference);
        super.visitReference(jcMemberReference);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        super.visitIdent(jcIdent);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcIdent);
        currentScope.enterExpression(jcIdent);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        super.visitLiteral(jcLiteral);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcLiteral);
        currentScope.enterExpression(jcLiteral);
    }
}
