package joust.optimisers.avail;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.normalisedexpressions.PossibleSymbol;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableBinary;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableFunctionalExpression;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableNullary;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableUnary;
import joust.utils.LogUtils;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A Scope that tracks PotentialAvailableExpressions as well as Symbols. Useful for Available
 * expression analysis.
 * TODO: Use efficient hashing similar to openjdk's Scope, instead of a brutal hack.
 */
public @Log4j2 class AvailScope extends Scope {
    // Maps VarSymbols to the expressions that depend on them. The outermost scope owns the object,
    // subsequent scopes point back to the same map. Each PAE has an associated virtual symbol.
    public HashMap<PossibleSymbol, Set<PotentiallyAvailableExpression>> symbolMap;

    // The set of currently available expressions in this innermost scope.
    public HashSet<PotentiallyAvailableExpression> availableExpressions;

    // A map relating trees to the corresponding PAEs.
    private HashMap<JCTree, PotentiallyAvailableExpression> treeMapping;

    // The symbols that are freshly-created in this scope.
    private ArrayList<PossibleSymbol> freshSyms = new ArrayList<>();

    public AvailScope(AvailScope enclosing, Symbol owner) {
        super(owner);
        next = enclosing;

        if (next != null) {
            symbolMap = enclosing.symbolMap;
            availableExpressions = enclosing.availableExpressions;
            treeMapping = enclosing.treeMapping;
        } else {
            symbolMap = new HashMap<>();
            availableExpressions = new HashSet<>();
            treeMapping = new HashMap<>();
        }
    }

    public AvailScope(Symbol owner) {
        this(null, owner);
    }

    // Override the enter method to initialise the dependency map for expressions as we go.
    @Override
    public void enter(Symbol sym) {
        enter(sym, this);
    }

    @Override
    public void enter(Symbol sym, Scope s) {
        enter(sym, s, s, false);
    }

    @Override
    public void enter(Symbol sym, Scope s, Scope s2, boolean b) {
        if (sym instanceof VarSymbol) {
            log.debug("Entering new map for {} as {}", sym, PossibleSymbol.getConcrete((VarSymbol) sym));
            symbolMap.put(PossibleSymbol.getConcrete((VarSymbol) sym), new HashSet<PotentiallyAvailableExpression>());
            freshSyms.add(PossibleSymbol.getConcrete((VarSymbol) sym));
        }

        super.enter(sym, s, s2, b);
    }

    /**
     * Helper method to enter a PAE into scope.
     *
     * @param expr The PAE to enter.
     * @param node The associated JCTree.
     */
    private void registerPotentialExpression(PotentiallyAvailableExpression expr, JCTree node) {
        log.debug("Register {} for node {}", expr, node);

        // Enter the virtual symbol into the symbol map (If it's a virtual symbol, that is.)
        if (!symbolMap.containsKey(expr.virtualSym)) {
            symbolMap.put(expr.virtualSym, new HashSet<PotentiallyAvailableExpression>());
            freshSyms.add(expr.virtualSym);
        }

        if (expr.concreteSym != null) {
            symbolMap.get(expr.concreteSym).add(expr);
        }

        // Add this symbol to the dependency list for each symbol it relies upon.
        for (PossibleSymbol sym : expr.deps) {
            symbolMap.get(sym).add(expr);
            log.debug("Symbols for {} are: {}", sym, Arrays.toString(symbolMap.get(sym).toArray()));
        }

        availableExpressions.add(expr);
        treeMapping.put(node, expr);

    }

    /**
     * Enter the given JCExpression into this scope.
     */
    public PotentiallyAvailableExpression enterExpression(JCExpression expr) {
        if (expr == null) {
            return null;
        }

        // I hate this so much.
        if (expr instanceof JCAssign) {
            return enterExpression((JCAssign) expr);
        } else if (expr instanceof JCAssignOp) {
            return enterExpression((JCAssignOp) expr);
        } else if (expr instanceof JCUnary) {
            return enterExpression((JCUnary) expr);
        } else if (expr instanceof JCBinary) {
            return enterExpression((JCBinary) expr);
        } else if (expr instanceof JCParens) {
            return enterExpression((JCParens) expr);
        } else if (expr instanceof JCIdent) {
            return enterExpression((JCIdent) expr);
        } else if (expr instanceof JCFieldAccess) {
            return enterExpression((JCFieldAccess) expr);
        } else if (expr instanceof JCLiteral) {
            return enterExpression((JCLiteral) expr);
        } else if (expr instanceof JCTypeCast) {
            return enterExpression((JCTypeCast) expr);
        } else if (expr instanceof JCInstanceOf) {
            return enterExpression((JCInstanceOf) expr);
        } else if (expr instanceof JCArrayAccess) {
            return enterExpression((JCArrayAccess) expr);
        } else if (expr instanceof JCMethodInvocation) {
            return enterExpression((JCMethodInvocation) expr);
        }

        return null;
    }

    public PotentiallyAvailableExpression enterExpression(JCParens expr) {
        LogUtils.raiseCompilerError("Unexpected JCParens entity encountered! " + expr);
        return null;
    }

    // TODO: Preprocessing step to eliminate chained-assignments.
    public PotentiallyAvailableExpression enterExpression(JCAssign expr) {
        // An assignment with =.
        // Break RHS into subexpressions and recurse on them.
        // Finally, identify and kill every expression depending on the LHS.
        VarSymbol sym = TreeUtils.getTargetSymbolForAssignment(expr);

        // Kill everything that depends on the symbol being assigned.
        killExpressionsDependingOnSymbol(PossibleSymbol.getConcrete(sym));

        // Fetch the already-entered expression for the rhs and update the concrete symbol there.
        PotentiallyAvailableExpression pae = treeMapping.get(expr.rhs);
        pae.setActualSymbol(sym);

        registerPotentialExpression(pae, expr);

        return pae;
    }

    public PotentiallyAvailableExpression enterExpression(JCAssignOp expr) {
        // An assignop kills the lhs, and you have to build your own expression representing the change.
        VarSymbol sym = TreeUtils.getTargetSymbolForAssignment(expr);

        // Get the concrete symbol for the target of this assignment.
        PossibleSymbol concreteSym = PossibleSymbol.getConcrete(sym);

        log.debug("Assignop funtimes for {}", concreteSym);
        // Find the expression already available in the assign, if we have one.
        PotentiallyAvailableExpression existingExpr = null;
        Set<PotentiallyAvailableExpression> mappings = symbolMap.get(concreteSym);
        for (PotentiallyAvailableExpression pae : mappings) {
            if (concreteSym.equals(pae.concreteSym)) {
                log.debug("Found existing PAE: {}", pae);
                existingExpr = pae;
            }
        }

        // Kill everything that depends on the symbol being assigned.
        killExpressionsDependingOnSymbol(concreteSym);

        if (existingExpr != null) {
            // Create and register a PAE representing existingExpr OP rhs.
            PotentiallyAvailableBinary newExpr = new PotentiallyAvailableBinary(existingExpr, treeMapping.get(expr.rhs), expr.getTag().noAssignOp());
            // TODO: Something something potential expression normaliser.
            newExpr.setActualSymbol(sym);

            // Recreate the symbol map...
            symbolMap.put(concreteSym, new HashSet<PotentiallyAvailableExpression>());

            registerPotentialExpression(newExpr, expr);
            return newExpr;
        }

        return null;
    }

    public PotentiallyAvailableExpression enterExpression(JCUnary expr) {
        PotentiallyAvailableExpression operand = treeMapping.get(expr.getExpression());

        PotentiallyAvailableUnary unary = new PotentiallyAvailableUnary(operand, expr.getTag());
        unary.sourceNode = expr;

        registerPotentialExpression(unary, expr);

        if (unary.opcode == Tag.PREINC
         || unary.opcode == Tag.PREDEC
         || unary.opcode == Tag.POSTINC
         || unary.opcode == Tag.POSTDEC) {
            killExpressionsDependingOnSymbol(PossibleSymbol.getConcrete(TreeUtils.getTargetSymbolForExpression(expr.getExpression())));
        }

        return unary;
    }


    public PotentiallyAvailableExpression enterExpression(JCBinary expr) {
        log.debug("entering JCBinary:" +expr);
        PotentiallyAvailableExpression lhs = treeMapping.get(expr.lhs);
        PotentiallyAvailableExpression rhs = treeMapping.get(expr.rhs);

        PotentiallyAvailableBinary binary = new PotentiallyAvailableBinary(lhs, rhs, expr.getTag());
        binary.sourceNode = expr;

        registerPotentialExpression(binary, expr);

        return binary;
    }

    public PotentiallyAvailableExpression enterExpression(JCIdent expr) {
        log.debug("entering JCIdent:" +expr);
        if (!(expr.sym instanceof VarSymbol)) {
            // log.warn("Encountered {} ident of unexpected type {}", expr, expr.sym.getClass().getCanonicalName());
            return null;
        }

        log.debug("JCIDent symbol:" +expr.sym);
        PotentiallyAvailableNullary nullary = new PotentiallyAvailableNullary(expr, (VarSymbol) expr.sym);
        nullary.sourceNode = expr;
        log.debug("JCIDent pae:" +nullary);

        registerPotentialExpression(nullary, expr);

        return nullary;
    }

    public PotentiallyAvailableExpression enterExpression(JCFieldAccess expr) {
        log.debug("entering JCFieldAccess:" +expr);
        if (!(expr.sym instanceof VarSymbol)) {
            log.warn("Encountered {} ident of unexpected type {}", expr, expr.sym.getClass().getCanonicalName());
            return null;
        }

        PotentiallyAvailableNullary nullary = new PotentiallyAvailableNullary(expr);
        nullary.sourceNode = expr;
        nullary.setActualSymbol((VarSymbol) expr.sym);

        registerPotentialExpression(nullary, expr);

        return nullary;
    }

    public PotentiallyAvailableExpression enterExpression(JCLiteral expr) {
        log.debug("entering JCLiteral:" +expr);
        PotentiallyAvailableNullary nullary = new PotentiallyAvailableNullary(expr);
        nullary.sourceNode = expr;

        registerPotentialExpression(nullary, expr);

        return nullary;
    }


    public PotentiallyAvailableExpression enterExpression(JCTypeCast expr) {
        log.debug("entering expression:" +expr);
        // TODO: Handle these...
        log.info("JCTypeCast not implemented in AvailScope.");
        return null;
    }

    public PotentiallyAvailableExpression enterExpression(JCInstanceOf expr) {
        log.debug("entering expression:" +expr);
        // TODO: Handle these... These can probably be considered as PotentiallyAvailableBinaries.
        log.info("JCInstanceOf not implemented in AvailScope.");
        return null;
    }

    public PotentiallyAvailableExpression enterExpression(JCArrayAccess expr) {
        log.debug("entering expression:" +expr);
        // TODO: Handle these... Fucking complicated.
        log.info("JCArrayAccess not implemented in AvailScope.");
        return null;
    }

    public PotentiallyAvailableExpression enterExpression(JCMethodInvocation expr) {
        log.debug("entering call:" +expr);
        PotentiallyAvailableFunctionalExpression nullary = new PotentiallyAvailableFunctionalExpression(expr);
        nullary.sourceNode = expr;

        // Tack on the PAEs (Previously generated) of the arguments to this call.
        for (JCExpression arg : expr.args) {
            PotentiallyAvailableExpression pae = treeMapping.get(arg);
            nullary.args = nullary.args.append(pae);
            nullary.deps.addAll(pae.deps);
        }
        log.debug("Created: {}", nullary);

        registerPotentialExpression(nullary, expr);

        return nullary;
    }

    /**
     * Get a Name that is not in use in this scope to be used for.
     * @return A Name currently unused in this scope. The naming convention ensures future collisions
     *         are impossible.
     */
    public Name getUnusedName() {
        Name proposedName;
        do {
            proposedName = NameFactory.getName();
        } while (!isSentinelEntry(lookup(proposedName)));

        return proposedName;
    }

    /**
     * Helper function to identify if a given Scope Entry is the sentinel node
     * @param e Entry to check.
     * @return True if e is the SENTINEL node defined in Scope, false otherwise.
     */
    private boolean isSentinelEntry(Entry e) {
        return e.scope == null && e.sym == null;
    }

    /**
     * Kill all expressions that depend on the given PossibleSymbol.
     *
     * @param pSym The possible symbol that is being invalidated.
     */
    public void killExpressionsDependingOnSymbol(PossibleSymbol pSym) {
        // If the symbol has already been killed, there is nothing to do.
        if (!symbolMap.containsKey(pSym)) {
            return;
        }

        log.debug("Killing things that depend on: {}", pSym);
        Set<PotentiallyAvailableExpression> dependsOnThis = new HashSet<>(symbolMap.get(pSym));
        symbolMap.get(pSym).clear();
        log.debug(dependsOnThis);
        for (PotentiallyAvailableExpression victim : dependsOnThis) {
            // Filter out virtual dependencies...
            for (PossibleSymbol sym : victim.deps) {
                if (sym.equals(pSym)) {
                    killExpression(victim);
                }
            }

            // Unset concrete symbol on things that were merely stored here...
            if (pSym.isConcrete()) {
                if (pSym.equals(victim.concreteSym)) {
                    victim.concreteSym = null;
                }
            }
        }
    }

    /**
     * Kill an expression and all expressions that depend on it.
     * If any dependency of an expression changes, it must be removed entirely.
     *
     * @param expr The expression to kill.
     */
    public void killExpression(PotentiallyAvailableExpression expr) {
        availableExpressions.remove(expr);

        log.debug("\nKilling: " + expr);

        killExpressionsDependingOnSymbol(expr.virtualSym);
    }

    @Override
    public AvailScope leave() {
        log.debug("Leaving scope!");
        // Remove all expressions that depended on symbols declared in this scope from the mapping.
        for (PossibleSymbol pSym : freshSyms) {
            killExpressionsDependingOnSymbol(pSym);
        }

        for (PossibleSymbol ps : freshSyms) {
            symbolMap.remove(ps);
        }
        freshSyms.clear();

        return (AvailScope) super.leave();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PotentiallyAvailableExpression e : availableExpressions) {
            sb.append(e.toString());
            sb.append('\n');
        }

        return sb.toString();
    }

    /**
     * Register the given VarSymbol with the PAE associated with the given tree, if any.
     * @param sym The symbol.
     * @param tree The tree.
     */
    public void registerSymbolWithExpression(VarSymbol sym, JCTree tree) {
        PotentiallyAvailableExpression expr = treeMapping.get(tree);

        if (expr != null) {
            expr.setActualSymbol(sym);
        }
    }

    /**
     * Redefine s1 to be equivalent to s2, updating the maps as appropriate.
     */
    public void mergeSymbols(PossibleSymbol s1, PossibleSymbol s2) {
        freshSyms.remove(s1);

        Set<PotentiallyAvailableExpression> s1Deps = symbolMap.get(s1);
        Set<PotentiallyAvailableExpression> s2Deps = symbolMap.get(s2);

        // Update every dependency on s1 to be a dependency on s2.
        for (PotentiallyAvailableExpression depExpr : s1Deps) {
            if (depExpr.virtualSym.equals(s1)) {
                depExpr.virtualSym = s2;
            }

            // The dependencies of the expression that depends on s1.
            Set<PossibleSymbol> depDeps = depExpr.deps;

            depDeps.remove(s1);
            depDeps.add(s2);
        }

        s2Deps.addAll(s1Deps);

        symbolMap.remove(s1);
    }
}
