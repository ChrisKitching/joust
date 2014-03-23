package joust.optimisers.visitors;

import com.sun.tools.javac.util.List;
import joust.utils.SymbolSet;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * Live variable analysis. Used on demand to determine the live variable set at each point.
 */
@Log4j2
public class Live extends BackwardsFlowVisitor {
    Set<VarSymbol> currentlyLive = new SymbolSet();

    // Every symbol that has ever been live.
    HashSet<VarSymbol> everLive = new HashSet<>();

    HashSet<VarSymbol> recentlyKilled;
    LinkedList<HashSet<VarSymbol>> recentlyKilledList = new LinkedList<HashSet<VarSymbol>>() {
        {
            recentlyKilled = new HashSet<>();
            push(recentlyKilled);
        }
    };

    private void enterBlock() {
        recentlyKilled = new HashSet<>();
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

    //TODO: Can we remove these?
    @Override
    public void visitDoWhileLoop(AJCDoWhileLoop jcDoWhileLoop) {
        enterBlock();
        super.visitDoWhileLoop(jcDoWhileLoop);
        leaveBlock();
    }

    @Override
    public void visitWhileLoop(AJCWhileLoop jcWhileLoop) {
        enterBlock();
        super.visitWhileLoop(jcWhileLoop);
        leaveBlock();
    }

    @Override
    public void visitForLoop(AJCForLoop jcForLoop) {
        enterBlock();
        super.visitForLoop(jcForLoop);
        leaveBlock();
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

        // And remove it from the in-progress killed set.
        recentlyKilled.removeAll(killedInThen);
        currentlyLive.removeAll(killedInThen);
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
                    killedEverywhere = new HashSet<>(recentlyKilled);
                } else {
                    // Every other time we throw away everything this new branch didn't kill.
                    killedEverywhere.retainAll(recentlyKilled);
                }

                // And then we restore the live set to the original state for the next case.
                leaveBlock();
            }
        }

        if (killedEverywhere != null && haveDefaultBranch) {
            recentlyKilled.addAll(killedEverywhere);
            currentlyLive.removeAll(killedEverywhere);
        }
    }

    // Stuff should start being live if it's live *anywhere* and stop being live if it stops *everywhere*.
    @Override
    public void visitMethodDef(AJCMethodDecl jcMethodDecl) {
        everLive = new HashSet<>();

        super.visitMethodDef(jcMethodDecl);

        jcMethodDecl.everLive = everLive;
    }

    @Override
    public void visitVariableDecl(AJCVariableDecl jcVariableDecl) {
        markLive(jcVariableDecl);
        if (!jcVariableDecl.getInit().isEmptyExpression()) {
            visit(jcVariableDecl.getInit());

            // If there's an assignment, we just killed this symbol (But may have added some, too!).
            VarSymbol referenced = jcVariableDecl.getTargetSymbol();
            currentlyLive.remove(referenced);
            recentlyKilled.add(referenced);
        }
    }

    @Override
    public void visitAssign(AJCAssign jcAssign) {
        visit(jcAssign.rhs);

        markLive(jcAssign);

        VarSymbol referenced = jcAssign.lhs.getTargetSymbol();
        currentlyLive.remove(referenced);
        recentlyKilled.add(referenced);
    }

    @Override
    public void visitAssignop(AJCAssignOp jcAssignOp) {
        visit(jcAssignOp.rhs);

        markLive(jcAssignOp);

        VarSymbol referenced = jcAssignOp.lhs.getTargetSymbol();
        currentlyLive.add(referenced);
    }

    @Override
    public void visitUnaryAsg(AJCUnaryAsg that) {
        log.debug("Arg is: {}", that.arg);
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
        VarSymbol referenced = (VarSymbol) tree.getTargetSymbol();
        if (referenced == null) {
            return;
        }

        currentlyLive.add(referenced);
        everLive.add(referenced);
    }

    private void markLive(AJCEffectAnnotatedTree tree) {
        // TODO: Datastructure-fu to enable portions of this map to be shared...
        tree.liveVariables = new SymbolSet(currentlyLive);
    }
}
