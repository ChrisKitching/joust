package joust.optimisers.cse;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import joust.optimisers.invar.ExpressionComplexityClassifier;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCComparableExpressionTree;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.logging.LogUtils;
import joust.utils.tree.NameFactory;
import joust.utils.data.SymbolSet;
import joust.utils.data.StackMap;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.tree.annotatedtree.AJCComparableExpressionTree.*;

/**
 * Visit all statements in a method in order, recording which expressions are available as you go along.
 * When an expression is ceasing to be available, determine if it's worth introducing a temporary variable for it and
 * do so if necessary.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class CommonSubExpressionTranslator extends BaseTranslator {
    // The minimum score for an expression to have to qualify for consideration.
    public static int MINIMUM_CSE_SCORE = 4;

    // A stack of maps for available expressions. Maps are pushed and popped as we enter/leave scopes.
    StackMap<AJCComparableExpressionTree, AvailableExpression> availableExpressions = new StackMap<>();

    // The set of AvailableExpressions ever live in this method - we look here when deciding what needs transforming
    // after completing our traversal of the method.
    List<AvailableExpression> everAvailInMethod = new ArrayList<>();

    private void enterScope() {
        availableExpressions.pushMap();
    }

    private void leaveScope() {
        Map<AJCComparableExpressionTree, AvailableExpression> popped = availableExpressions.popMap();

        // Finalise the popped expressions...
        Set<AJCComparableExpressionTree> keysCopy = new HashSet<>(popped.keySet());
        for (AJCComparableExpressionTree t : keysCopy) {
            log.debug("Key: {}", t);
            finaliseAvailableExpression(popped.get(t));
        }
    }

    @Override
    protected void visitClassDef(AJCClassDecl that) {
        visit(that.methods);
        visit(that.classes);
    }

    @Override
    protected void visitBlock(AJCBlock that) {
        enterScope();
        super.visitBlock(that);
        leaveScope();
    }

    @Override
    protected void visitMethodDef(AJCMethodDecl that) {
        Symbol.MethodSymbol enclosingMethod = that.getTargetSymbol();

        // Clear out any junk from the previous run.
        availableExpressions.clear();
        everAvailInMethod.clear();

        super.visitMethodDef(that);

        // Visit forwards.
        // Visit all expressions of high enough value.
        // Keep an available expression set of things of enough value.

        everAvailInMethod.sort(new AvailableExpressionComparator());

        if (everAvailInMethod.isEmpty()) {
            return;
        }

        log.info("CSE visit for: {}", that);

        AvailableExpression targetExpr = everAvailInMethod.get(0);
        log.info("Most valuable expression: {} with score {}", targetExpr, targetExpr.getComplexityScore());

        AJCExpressionTree expr = targetExpr.firstInstance.wrappedNode;

        // Name and symbol for new temporary variable.
        Name tempName = NameFactory.getName();
        Symbol.VarSymbol newSym = new Symbol.VarSymbol(Flags.FINAL, tempName, expr.getNodeType(), enclosingMethod);

        // Declaration thereof.
        AJCVariableDecl newDecl = treeMaker.VarDef(newSym, treeCopier.copy(expr));

        // Insert the new declaration before the first use.
        log.info("Need enclosing statement for: {}:{}", expr, expr.getClass().getCanonicalName());

        expr.getEnclosingBlock().insertBefore(expr.getEnclosingStatement(), newDecl);

        // Replace uses of the expression with a reference to the new temporary variable.
        for (AJCComparableExpressionTree use : targetExpr.usages) {
            use.wrappedNode.swapFor(treeMaker.Ident(newSym));
        }

        mHasMadeAChange = true;

        log.info("After CSE pass:\n{}", expr.getEnclosingBlock());

        // We changed the tree. Rerun side effect analysis and do it all over again. (Hopefully not forever...)
        AJCForest.getInstance().initialAnalysis();
        visitMethodDef(that);
    }

    /**
     * General visitor function for considering expressions.
     *
     * @param that The expression to consider.
     * @return true if the visitor should continue down the tree, false if this branch should be explored no further.
     *         (Used to avoid exploring down trees that are already too inexpensive to be interesting).
     */
    private void visitExpression(AJCComparableExpressionTree<? extends AJCExpressionTree> that) {
        // Determine if this tree is cheap enough that we don't care about it.
        ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
        classifier.visitTree(that.wrappedNode);

        log.debug("Encountered: {}", that);

        int score = classifier.getScore();
        if (score < MINIMUM_CSE_SCORE) {
            log.debug("Score too low - stop.");
            return;
        }

        // Determine if this tree holds side effects that prevent us from being able to consider it for CSE.
        EffectSet effects = that.wrappedNode.effects.getEffectSet();
        if (effects.contains(EffectSet.EffectType.WRITE_ESCAPING)
         || effects.contains(EffectSet.EffectType.READ_ESCAPING) // Concurrency...
         || effects.contains(EffectSet.EffectType.IO)) {
            log.debug("Effects problematic - stop.");
            return;
        }

        // Do we have an AvailableExpression for this already?
        // Need stack of Maps from expressions to AvailableExpressions.
        if (availableExpressions.containsKey(that)) {
            AvailableExpression existingExpr = availableExpressions.get(that);
            existingExpr.addUsage(that);

            log.debug("Existing key for {} found. Now: {}", that, existingExpr);
        } else {
            AvailableExpression expr = new AvailableExpression(that);
            log.debug("New entry: {}", expr);
            availableExpressions.put(that, expr);
        }
    }

    /**
     * General visitor method for handling updates - strips from the availableExpressions map anything invalidated by
     * this tree. (Using effect information).
     */
    private void visitUpdate(AJCEffectAnnotatedTree tree) {
        EffectSet effects = tree.effects.getEffectSet();
        SymbolSet internalWrites = effects.writeInternal;

        // If this tree writes anything that is read by an availableExpression, it has to go.
        // TODO: Make this more efficient.
        Set<AJCComparableExpressionTree> keysCopy = new HashSet<>(availableExpressions.keySet());
        for (AJCComparableExpressionTree t : keysCopy) {
            if (t.wrappedNode instanceof AJCEffectAnnotatedTree) {
                AJCEffectAnnotatedTree effectTree = (AJCEffectAnnotatedTree) t.wrappedNode;

                EffectSet availEffects = effectTree.effects.getEffectSet();
                for (Symbol.VarSymbol sym : availEffects.readInternal) {
                    if (internalWrites.contains(sym)) {
                        log.debug("Dropping {} because {} wrote {}", t, tree, sym);
                        finaliseAvailableExpression(availableExpressions.get(t));
                        availableExpressions.remove(t);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Determine if a given available expression provides enough value to be worth swapping.
     * If it does, add it to the everAvailInMethod set. Otherwise, don't.
     */
    private void finaliseAvailableExpression(AvailableExpression expr) {
        log.debug("Finalising {}", expr);
        if (expr.usages.size() <= 1) {
            log.debug("One use. No point.");
            return;
        }

        int score = expr.getComplexityScore();
        int afterScore = expr.getApplicationCost();

        log.debug("beforeScore: {}", score);
        log.debug("afterScore:  {}", afterScore);

        if (afterScore >= score) {
            log.debug("No point... :(");
            // No point...
            return;
        }

        everAvailInMethod.add(expr);
    }

    @Override
    protected void visitFieldAccess(AJCFieldAccess that) {
        if (that.getTargetSymbol() instanceof Symbol.VarSymbol) {
            visitExpression(new ComparableAJCSymbolRefTree(that));
            super.visitFieldAccess(that);
        }
    }

    @Override
    protected void visitBinary(AJCBinary that) {
        visitExpression(new ComparableAJCBinary(that));
        super.visitBinary(that);
    }

    @Override
    protected void visitUnary(AJCUnary that) {
        visitExpression(new ComparableAJCUnary(that));
        super.visitUnary(that);
    }

    @Override
    protected void visitUnaryAsg(AJCUnaryAsg that) {
        visitUpdate(that);
        visitExpression(new ComparableAJCUnaryAsg(that));
        super.visitUnaryAsg(that);
    }

    @Override
    protected void visitTypeCast(AJCTypeCast that) {
        visitExpression(new ComparableAJCTypeCast(that));
        super.visitTypeCast(that);
    }

    @Override
    protected void visitInstanceOf(AJCInstanceOf that) {
        visitExpression(new ComparableAJCInstanceOf(that));
        super.visitInstanceOf(that);
    }

    @Override
    protected void visitCall(AJCCall that) {
        visitUpdate(that);
        visitExpression(new ComparableAJCCall(that));
        super.visitCall(that);
    }

    @Override
    protected void visitArrayAccess(AJCArrayAccess that) {
        visitUpdate(that);
        super.visitArrayAccess(that);
    }

    @Override
    protected void visitAssignop(AJCAssignOp that) {
        visitUpdate(that);
        super.visitAssignop(that);
    }

    @Override
    protected void visitAssign(AJCAssign that) {
        visitUpdate(that);
        super.visitAssign(that);
    }

    @Override
    protected void visitDoWhileLoop(AJCDoWhileLoop that) {
        visitUpdate(that);
        super.visitDoWhileLoop(that);
    }

    @Override
    protected void visitWhileLoop(AJCWhileLoop that) {
        visitUpdate(that);
        super.visitWhileLoop(that);
    }

    @Override
    protected void visitForLoop(AJCForLoop that) {
        visitUpdate(that);
        super.visitForLoop(that);
    }

    @Override
    protected void visitSwitch(AJCSwitch that) {
        // Because of the stupid scoping rules, this needs special treatment.
        // You enter a scope when you hit the first case, and you leave it when you hit a break.
        visit(that.selector);

        // For the purposes of CSE, you want to throw away newly-available expressions between cases.
        for (AJCCase cas : that.cases) {
            enterScope();
            visitCase(cas);
            leaveScope();
        }
    }
}
