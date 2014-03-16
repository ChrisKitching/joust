package joust.optimisers.avail;

import com.esotericsoftware.minlog.Log;
import com.sun.tools.javac.tree.JCTree;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitorImpl;
import joust.treeinfo.EffectSet;
import lombok.extern.log4j.Log4j2;


import java.util.Arrays;
import java.util.HashSet;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Perform available expression analysis on methods.
 */
@Log4j2
public class Avail extends AJCTreeVisitorImpl {
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
    private void markAvailableExpressions(AJCEffectAnnotatedTree tree) {
        HashSet<PotentiallyAvailableExpression> avail = new HashSet<>();
        avail.addAll(currentScope.availableExpressions);
        log.info("Marking available for {} as:\n{}", tree, Arrays.toString(avail.toArray()));
        tree.avail = avail;
    }

    @Override
    public void visitMethodDef(AJCMethodDecl jcMethodDecl) {
        log.debug("Visitng method: " + jcMethodDecl);
        enclosingMethod = jcMethodDecl.getTargetSymbol();
        currentScope = new AvailScope(jcMethodDecl.getTargetSymbol());

        // TODO: Something something nested classes.
        // TODO: Possibly need to prepopulate class scope at this point to avoid collisions...
        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl jcVariableDecl) {
        markAvailableExpressions(jcVariableDecl);

        // Enter the symbol.
        currentScope.enter(jcVariableDecl.getTargetSymbol());

        super.visitVariableDecl(jcVariableDecl);

        // Associate the concrete symbol with the PAE for the initialiser, if any exists.
        AJCTree init = jcVariableDecl.getInit();
        if (init != null) {
            currentScope.registerSymbolWithExpression(jcVariableDecl.getTargetSymbol(), init);
        }
    }

    @Override
    public void visitBlock(AJCBlock jcBlock) {
        log.info("Entering block for Avail: {}", jcBlock);
        boolean blockWasNewScope = nextBlockIsNewScope;
        if (nextBlockIsNewScope) {
            log.info("Entering block scope: {}", jcBlock);
            enterScope();
        } else {
            nextBlockIsNewScope = true;
        }

        super.visitBlock(jcBlock);
        log.debug("After block {}:\nWe have:\n{}", jcBlock, currentScope);

        if (blockWasNewScope) {
            log.info("Exiting block scope: {}", jcBlock);
            leaveScope();
        }
    }

    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop jcDoWhileLoop) {
        markAvailableExpressions(jcDoWhileLoop);
        super.visitDoWhileLoop(jcDoWhileLoop);
    }

    @Override
    public void visitForLoop(AJCForLoop jcForLoop) {
        markAvailableExpressions(jcForLoop);
        enterScope();
        nextBlockIsNewScope = false;
        super.visitForLoop(jcForLoop);
        leaveScope();
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop jcWhileLoop) {
        markAvailableExpressions(jcWhileLoop);
        log.debug("Entering visitWhileLoop with scope:\n{}", currentScope);

        super.visitWhileLoop(jcWhileLoop);
        log.debug("Exiting visitWhileLoop with scope:\n{}", currentScope);
    }

    // TODO: Unused? Wat.
    @Override
    public void visitCase(AJCCase jcCase) {
        // Although each case is in the same scope as the other cases, the "Scope" we're using here
        // is a crazy munging of lexical scope and flow analysis. This is safe. Honest.
        enterScope();
        super.visitCase(jcCase);
        leaveScope();
    }

    // TODO: Something something PolyExpression.
    @Override
    public void visitConditional(AJCConditional jcConditional) {
        markAvailableExpressions(jcConditional);
        super.visitConditional(jcConditional);
    }

    @Override
    public void visitExpressionStatement(AJCExpressionStatement jcExpressionStatement) {
        markAvailableExpressions(jcExpressionStatement);
        super.visitExpressionStatement(jcExpressionStatement);
    }

    @Override
    public void visitAssign(AJCAssign jcAssign) {
        Log.debug("Visit assign!");
        markAvailableExpressions(jcAssign);
        visit(jcAssign.rhs);
        currentScope.enterExpression(jcAssign);
    }

    @Override
    public void visitAssignop(AJCAssignOp jcAssignOp) {
        markAvailableExpressions(jcAssignOp);
        if (!eligibleForCSE(jcAssignOp)) {
            return;
        }
        super.visitAssignop(jcAssignOp);
        currentScope.enterExpression(jcAssignOp);
    }

    @Override
    public void visitUnary(AJCUnary jcUnary) {
        markAvailableExpressions(jcUnary);
        if (!eligibleForCSE(jcUnary)) {
            return;
        }
        super.visitUnary(jcUnary);
        currentScope.enterExpression(jcUnary);
    }

    @Override
    public void visitBinary(AJCBinary jcBinary) {
        markAvailableExpressions(jcBinary);
        if (!eligibleForCSE(jcBinary)) {
            return;
        }
        super.visitBinary(jcBinary);
        currentScope.enterExpression(jcBinary);
    }

    @Override
    public void visitTypeCast(AJCTypeCast jcTypeCast) {
        markAvailableExpressions(jcTypeCast);
        if (!eligibleForCSE(jcTypeCast)) {
            return;
        }
        super.visitTypeCast(jcTypeCast);
        currentScope.enterExpression(jcTypeCast);
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf jcInstanceOf) {
        markAvailableExpressions(jcInstanceOf);
        if (!eligibleForCSE(jcInstanceOf)) {
            return;
        }
        super.visitInstanceOf(jcInstanceOf);
        currentScope.enterExpression(jcInstanceOf);
    }

    @Override
    public void visitArrayAccess(AJCArrayAccess jcArrayAccess) {
        markAvailableExpressions(jcArrayAccess);
        if (!eligibleForCSE(jcArrayAccess)) {
            return;
        }
        super.visitArrayAccess(jcArrayAccess);
        currentScope.enterExpression(jcArrayAccess);
    }

    @Override
    public void visitFieldAccess(AJCFieldAccess jcFieldAccess) {
        markAvailableExpressions(jcFieldAccess);
        super.visitFieldAccess(jcFieldAccess);
    }

    /**
     * Mostly a hack to keep invariant code motion happy (So the last statement in the loop body has as available
     * expressions all the things calculated in the last *actual* statement of the loop body (Which the skip has been
     * added following.)
     */
    @Override
    public void visitSkip(AJCSkip jcSkip) {
        markAvailableExpressions(jcSkip);
        super.visitSkip(jcSkip);
    }

    @Override
    public void visitIdent(AJCIdent jcIdent) {
        super.visitIdent(jcIdent);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcIdent);
        currentScope.enterExpression(jcIdent);
    }

    @Override
    public void visitLiteral(AJCLiteral jcLiteral) {
        super.visitLiteral(jcLiteral);
        if (currentScope == null) {
            return;
        }
        markAvailableExpressions(jcLiteral);
        currentScope.enterExpression(jcLiteral);
    }

    @Override
    public void visitCall(AJCCall jcMethodInvocation) {
        markAvailableExpressions(jcMethodInvocation);
        if (!eligibleForCSE(jcMethodInvocation)) {
            return;
        }
        super.visitCall(jcMethodInvocation);

        currentScope.enterExpression(jcMethodInvocation);
    }

    /**
     * Helper function to determine if a particular node is eligible for CSE using effect information.
     */
    private boolean eligibleForCSE(AJCEffectAnnotatedTree tree) {
        EffectSet effects = tree.effects.getEffectSet();
        log.warn("Effects for {} are {}", tree, effects);

        // It's safe to consider a node for common subexpression elimination if it has either escaping
        // writes or escaping reads, but not if it has both.
        if (effects.contains(EffectSet.EffectType.READ_ESCAPING)
         && effects.contains(EffectSet.EffectType.WRITE_ESCAPING)) {
            return false;
        }

        // Naturally, IO also needs to be retained...
        if (effects.contains(EffectSet.EffectType.IO)) {
            return false;
        }

        return true;
    }
}
