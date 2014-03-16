package joust.optimisers.translators;

import joust.optimisers.evaluation.Value;
import lombok.extern.log4j.Log4j2;

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
@Log4j2
public
class ConstFoldTranslator extends BaseTranslator {
    @Override
    public void visitUnary(AJCUnary tree) {
        super.visitUnary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        final AJCExpression expr = tree.arg;

        // Replace each unary operation on a literal with a literal of the new value.
        if (!(expr instanceof AJCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value operand = Value.of(((AJCLiteral) expr).getValue());
        tree.swapFor(Value.unary(nodeTag, operand).toLiteral());
    }

    @Override
    public void visitBinary(AJCBinary tree) {
        super.visitBinary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        AJCExpression leftOperand = tree.lhs;
        AJCExpression rightOperand = tree.rhs;

        // Ensure this is an operation on literals before proceeding.
        if (!(leftOperand instanceof AJCLiteral && rightOperand instanceof AJCLiteral)) {
            return;
        }

        mHasMadeAChange = true;

        // To Values...
        Value lValue = Value.of(((AJCLiteral) leftOperand).getValue());
        Value rValue = Value.of(((AJCLiteral) rightOperand).getValue());

        tree.swapFor(Value.binary(nodeTag, lValue, rValue).toLiteral());
    }
}
