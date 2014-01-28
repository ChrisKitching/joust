package joust.optimisers.translators;

import joust.optimisers.evaluation.Value;
import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
/**
 * Implementation of constant folding. Unfortunately, javac does its constant folding after we are
 * invoked, and it does so, perhaps understandably, at a lower level than the AST.
 * The objective here is to get constant folding done at the AST level so other, actually
 * interesting optimisations are not hindered by the non-foldedness of constants when they
 * subsequently are run.
 * Unfortunately, doing things at this level means we have to deal with issues like brackets.
 * TODO: Lambdas might be able to make this less soul-destroyingly awful to look at.
 */
public @Log4j2
class ConstFoldTranslator extends BaseTranslator {
    public void visitUnary(JCUnary tree) {
        super.visitUnary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        final JCExpression expr = tree.getExpression();

        // Replace each unary operation on a literal with a literal of the new value.
        if (!(expr instanceof JCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value operand = Value.of(((JCLiteral) expr).getValue());
        result = Value.unary(nodeTag, operand).toLiteral();

    }

    public void visitBinary(JCBinary tree) {
        super.visitBinary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        JCExpression leftOperand = tree.getLeftOperand();
        JCExpression rightOperand = tree.getRightOperand();

        // Ensure this is an operation on literals before proceeding.
        if (!(leftOperand instanceof JCLiteral && rightOperand instanceof JCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value lValue = Value.of(((JCLiteral) leftOperand).getValue());
        Value rValue = Value.of(((JCLiteral) rightOperand).getValue());
        result = Value.binary(nodeTag, lValue, rValue).toLiteral();
    }

    /**
     * Remove parens that are wrapping a lone literal.
     *
     * @param tree JCParens node to examine.
     */
    @Override
    public void visitParens(JCParens tree) {
        super.visitParens(tree);

        JCExpression inside = tree.getExpression();
        if (inside instanceof JCLiteral) {
            mHasMadeAChange = true;
            result = inside;
        }
    }

}
