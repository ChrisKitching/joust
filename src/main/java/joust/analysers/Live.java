package joust.analysers;

import com.sun.tools.javac.util.List;
import joust.utils.logging.LogUtils;
import joust.utils.data.SymbolSet;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Live variable analysis. Used on demand to determine the live variable set at each point.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class Live extends BackwardsFlowVisitor {
    // If we're currently processing inside a loop, this flag is set.
    private boolean withinLoop = false;

    // The set of symbols that are freshly declared within this loop. These are the only symbols that may be killed
    // while we're within the current loop.
    Set<VarSymbol> symbolWhitelist;

    Set<VarSymbol> currentlyLive = new SymbolSet();

    // Every symbol that has ever been live.
    HashSet<VarSymbol> everLive = new HashSet<VarSymbol>();

    HashSet<VarSymbol> recentlyKilled;
    LinkedList<HashSet<VarSymbol>> recentlyKilledList = new LinkedList<HashSet<VarSymbol>>() {
        {
            recentlyKilled = new HashSet<VarSymbol>();
            push(recentlyKilled);
        }
    };

    private void enterBlock() {
        recentlyKilled = new HashSet<VarSymbol>();
        recentlyKilledList.push(recentlyKilled);
    }

    /**
     * Recent the currentlyLive set for visiting a new block and set the recentlyKilled set to the next outer
     * scope (And throw away the current one).
     */
    private void leaveBlock() {
        currentlyLive.addAll(recentlyKilled);
        recentlyKilledList.pop();
        recentlyKilled = recentlyKilledList.peek();
    }

    private void kill(VarSymbol sym) {
        if (withinLoop && !symbolWhitelist.contains(sym)) {
            return;
        }

        if (!currentlyLive.contains(sym)) {
            return;
        }

        recentlyKilled.add(sym);
        currentlyLive.remove(sym);
    }
    private void kill(Set<VarSymbol> syms) {
        if (withinLoop) {
            syms.retainAll(symbolWhitelist);
        }

        // Drop those that aren't currently live...
        Set<VarSymbol> copy = new HashSet<VarSymbol>(syms);
        copy.retainAll(currentlyLive);

        // And remove it from the in-progress killed set.
        recentlyKilled.addAll(copy);
        currentlyLive.removeAll(copy);
    }

    // Visit loops twice, so the liveness between loops is considered...
    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop jcDoWhileLoop) {
        boolean wasInLoop = enterLoop(jcDoWhileLoop);

        enterBlock();
        super.visitDoWhileLoop(jcDoWhileLoop);
        leaveBlock();

        withinLoop = wasInLoop;
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop jcWhileLoop) {
        boolean wasInLoop = enterLoop(jcWhileLoop);

        enterBlock();
        super.visitWhileLoop(jcWhileLoop);
        leaveBlock();

        withinLoop = wasInLoop;
    }

    @Override
    public void visitForLoop(AJCForLoop jcForLoop) {
        // Loops are annoying - stuff can be made live within the loop that isn't live after the loop and which is
        // assigned within the loop, later than the read. That assignment needs to be kept, though, but possibly
        // redundant ones that shadow it might not.
        // Fun.

        boolean wasInLoop = enterLoop(jcForLoop);

        enterBlock();
        super.visitForLoop(jcForLoop);
        leaveBlock();

        withinLoop = wasInLoop;
    }

    /**
     * Update the state as required to enter a loop.
     * @param loop The loop node we're about to process.
     * @return If the loop has readInternal == UNIVERSAL_SET.
     */
    private boolean enterLoop(AJCEffectAnnotatedTree loop) {
        AssignmentLocator asgLocator = new AssignmentLocator();
        asgLocator.visitTree(loop);

        symbolWhitelist = asgLocator.declarations;

        boolean wasInLoop = withinLoop;
        withinLoop = true;

        // Add to the live set every variable read somewhere in the loop that isn't declared within the loop.
        // TODO: This makes us miss some redundant assignments... :(

        // Do evil stupidity to locate the set of locally-touched symbols... TODO: Different EffectSet representation?
        TouchedSymbolLocator touch = new TouchedSymbolLocator();
        touch.visitTree(loop);
        Set<VarSymbol> syms = touch.touched;

        if (syms != null) {
            Set<VarSymbol> symCopy = new HashSet<VarSymbol>(syms);
            symCopy.removeAll(symbolWhitelist);

            currentlyLive.addAll(symCopy);
        }

        return wasInLoop;
    }

    @Override
    public void visitIf(AJCIf jcIf) {
        // New recently-killed set created...
        enterBlock();

        // Populate it with the stuff killed by the elsepart.
        visit(jcIf.elsepart);

        // Keep a reference to the stuff killed by else and strip it from the list.
        HashSet<VarSymbol> killedInElse = recentlyKilled;
        leaveBlock();

        // New recently-killed set...
        enterBlock();
        visit(jcIf.thenpart);

        HashSet<VarSymbol> killedInThen = recentlyKilled;
        leaveBlock();

        // We now have the original recentlyKilled set (The one for the context containing this if) once again.
        // We now want to figure out what changes this if made to it - any symbols killed in both branches are really
        // dead now.

        // Take the intersection of the kill sets...
        killedInThen.retainAll(killedInElse);

        kill(killedInThen);

        visit(jcIf.cond);
    }

    @Override
    public void visitSwitch(AJCSwitch jcSwitch) {
        // Tracks things killed in every branch. Such things, if there also exists a default branch, may be culled.
        HashSet<VarSymbol> killedEverywhere = null;
        boolean haveDefaultBranch = false;
        for (List<AJCCase> l = jcSwitch.cases; l.nonEmpty(); l = l.tail) {
            if (l.head != null) {
                if (l.head.pat.isEmptyExpression()) {
                    haveDefaultBranch = true;
                }

                enterBlock();

                visit(l.head);
                // Drop everything that was killed in the other case but not in this one (We want ones killed
                // everywhere only).
                if (killedEverywhere == null) {
                    // First time we set it to the kill set from the first branch.
                    killedEverywhere = new HashSet<VarSymbol>(recentlyKilled);
                } else {
                    // Every other time we throw away everything this new branch didn't kill.
                    killedEverywhere.retainAll(recentlyKilled);
                }

                // And then we restore the live set to the original state for the next case.
                leaveBlock();
            }
        }

        if (killedEverywhere != null && haveDefaultBranch) {
            kill(killedEverywhere);
        }

        visit(jcSwitch.selector);
    }

    @Override
    public void visitTry(AJCTry jcTry) {
        // If there's a finaliser, the analysis of everything else takes place in series with it.
        enterBlock();
        visit(jcTry.finalizer);
        HashSet<VarSymbol> finaliserDeaths = new HashSet<VarSymbol>(recentlyKilled);
        leaveBlock();

        finaliserDeaths.retainAll(currentlyLive);

        // If it's killed in the finaliser, it's dead in the catchers/body.
        recentlyKilled.addAll(finaliserDeaths);
        currentlyLive.removeAll(finaliserDeaths);

        // Find the things killed in every catcher, similarly to how we handle switch cases.
        HashSet<VarSymbol> catcherDeaths = null;
        for (AJCCatch catcher : jcTry.catchers) {
            enterBlock();
            visit(catcher);

            // Track everything that is killed in every catcher.
            if (catcherDeaths == null) {
                catcherDeaths = new HashSet<VarSymbol>(recentlyKilled);
            } else {
                catcherDeaths.retainAll(recentlyKilled);
            }

            // And then we restore the live set to the original state for the next one.
            leaveBlock();
        }

        enterBlock();
        visit(jcTry.body);
        HashSet<VarSymbol> bodyDeaths = new HashSet<VarSymbol>(recentlyKilled);

        leaveBlock();

        // If there are no catchers, we can't kill anything at all - it'll jump from the try to the finally at some
        // point. Without a system to determine where that jump occurs, we're stuck. TODO: That!
        if (catcherDeaths == null) {
            return;
        }

        // If it's dead in all catchers and the body, it's also dead.
        bodyDeaths.retainAll(catcherDeaths);

        kill(bodyDeaths);
    }

    // Stuff should start being live if it's live *anywhere* and stop being live if it stops *everywhere*.
    @Override
    public void visitMethodDef(AJCMethodDecl jcMethodDecl) {
        everLive = new HashSet<VarSymbol>();

        super.visitMethodDef(jcMethodDecl);

        jcMethodDecl.everLive = everLive;
    }

    @Override
    public void visitArrayAccess(AJCArrayAccess jcArrayAccess) {
        super.visitArrayAccess(jcArrayAccess);
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl jcVariableDecl) {
        if (!jcVariableDecl.getInit().isEmptyExpression()) {
            visit(jcVariableDecl.getInit());
        }

        markLive(jcVariableDecl);

        if (!jcVariableDecl.getInit().isEmptyExpression()) {
            // If there's an assignment, we just killed this symbol (But may have added some, too!).
            VarSymbol referenced = jcVariableDecl.getTargetSymbol();
            kill(referenced);
        }
    }

    @Override
    public void visitAssign(AJCAssign jcAssign) {
        log.debug("VisitAsg: {}", jcAssign);
        log.debug(Arrays.toString(currentlyLive.toArray()));
        visit(jcAssign.rhs);

        markLive(jcAssign);

        // Array accesses aren't *really* writes.
        if (jcAssign.lhs instanceof AJCArrayAccess) {
            return;
        }

        VarSymbol referenced = jcAssign.lhs.getTargetSymbol();

        kill(referenced);
    }

    @Override
    public void visitAssignop(AJCAssignOp jcAssignOp) {
        log.debug("VisitAsgOp: {}", jcAssignOp);
        log.debug(Arrays.toString(currentlyLive.toArray()));
        visit(jcAssignOp.rhs);

        markLive(jcAssignOp);

        VarSymbol referenced = jcAssignOp.lhs.getTargetSymbol();
        currentlyLive.add(referenced);
    }

    @Override
    public void visitUnaryAsg(AJCUnaryAsg that) {
        log.debug("Arg is: {}", that.arg);
        log.debug(Arrays.toString(currentlyLive.toArray()));
        markLive(that);
        visit(that.arg);
    }

    @Override
    public void visitFieldAccess(AJCFieldAccess jcFieldAccess) {
        super.visitFieldAccess(jcFieldAccess);
        processReference(jcFieldAccess);
    }

    @Override
    public void visitIdent(AJCIdent jcIdent) {
        super.visitIdent(jcIdent);
        processReference(jcIdent);
    }

    /**
     * Consider an expression that may refer to a VarSymbol and add it to the currently live set.
     * @param tree The JCIdent or JCFieldAccess to consider.
     */
    private void processReference(AJCSymbolRef tree) {
        if (!(tree.getTargetSymbol() instanceof VarSymbol)) {
            return;
        }
        log.debug("Visit ref: {}", tree);
        log.debug(Arrays.toString(currentlyLive.toArray()));

        VarSymbol referenced = (VarSymbol) tree.getTargetSymbol();

        currentlyLive.add(referenced);
        everLive.add(referenced);
    }

    private void markLive(AJCEffectAnnotatedTree tree) {
        // TODO: Datastructure-fu to enable portions of this map to be shared...
        tree.liveVariables = new SymbolSet(currentlyLive);
    }
}
