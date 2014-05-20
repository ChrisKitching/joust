package joust.optimisers.translators;

import joust.tree.annotatedtree.AJCComparableExpressionTree;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import joust.utils.tree.evaluation.Value;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.Tag;
import static joust.tree.annotatedtree.AJCTree.*;
/**
 * Implementation of constant folding. Unfortunately, javac does its constant folding after we are
 * invoked, and it does so, perhaps understandably, at a lower level than the AST.
 * The objective here is to get constant folding done at the AST level so other, actually
 * interesting optimisations are not hindered by the non-foldedness of constants when they
 * subsequently are run.
 * Unfortunately, doing things at this level means we have to deal with issues like brackets.
 * TODO: Lambdas might be able to make this less soul-destroyingly awful to look at.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ConstFoldTranslator extends BaseTranslator {
    @Override
    public void visitUnary(AJCUnary tree) {
        super.visitUnary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        final AJCExpressionTree expr = tree.arg;

        // Replace each unary operation on a literal with a literal of the new value.
        if (!(expr instanceof AJCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value operand = Value.of(((AJCLiteral) expr).getValue());
        AJCLiteral replacement = Value.unary(nodeTag, operand).toLiteral();

        try {
            tree.swapFor(replacement);
        } catch (NoSuchElementException e) {
            // The way javac constructs literal arrays wil lcause us to get here. No matter.
        }

        AJCForest.getInstance().increment("Constants Folded: ");
        log.info("{}:{} -> {}:{}", tree, tree.getClass().getCanonicalName(), replacement, replacement.getClass().getCanonicalName());
    }

    @Override
    public void visitBinary(AJCBinary tree) {
        super.visitBinary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        AJCExpressionTree leftOperand = tree.lhs;
        AJCExpressionTree rightOperand = tree.rhs;

        // Ensure this is an operation on literals before proceeding.
        if (!(leftOperand instanceof AJCLiteral && rightOperand instanceof AJCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value lValue = Value.of(((AJCLiteral) leftOperand).getValue());
        Value rValue = Value.of(((AJCLiteral) rightOperand).getValue());

        AJCLiteral replacement = Value.binary(nodeTag, lValue, rValue).toLiteral();

        try {
            tree.swapFor(replacement);
        } catch (NoSuchElementException e) {
            // The way javac constructs literal arrays wil lcause us to get here. No matter.
        }

        AJCForest.getInstance().increment("Constants Folded: ");
        log.info("{} {} {} -> {}", leftOperand, nodeTag, rightOperand, replacement);
    }

    @Override
    protected void visitConditional(AJCConditional that) {
        // Fold the subtrees of the conditional...
        super.visitConditional(that);

        if (that.cond instanceof AJCLiteral) {
            AJCLiteral cast = (AJCLiteral) that.cond;
            Object val = cast.getValue();
            if (!(val instanceof Boolean)) {
                log.fatal("Non-boolean boolean literal detected: {}", that.cond);
                return;
            }

            boolean realValue = (Boolean) val;
            if (realValue) {
                that.swapFor(that.truepart);
            } else {
                that.swapFor(that.falsepart);
            }
            AJCForest.getInstance().increment("Conditionals Folded: ");
        }
    }

    @Override
    protected void visitIf(AJCIf that) {
        super.visitIf(that);

        if (that.cond instanceof AJCLiteral) {
            AJCLiteral cast = (AJCLiteral) that.cond;
            Object val = cast.getValue();
            if (!(val instanceof Boolean)) {
                log.fatal("Non-boolean boolean literal detected: {}", that.cond);
                return;
            }

            boolean realValue = (Boolean) val;
            if (realValue) {
                that.swapFor(that.thenpart);
            } else {
                that.swapFor(that.elsepart);
            }
            AJCForest.getInstance().increment("Conditionals Folded: ");
        }
    }
}
