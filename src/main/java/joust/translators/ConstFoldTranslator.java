package joust.translators;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import joust.Optimiser;
import joust.utils.LogUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.sun.tools.javac.tree.JCTree.Tag.*;
import static com.sun.tools.javac.tree.JCTree.*;
import static joust.Optimiser.treeMaker;
/**
 * Implementation of constant folding. Unfortunately, javac does its constant folding after we are
 * invoked, and it does so, perhaps understandably, at a lower level than the AST.
 * The objective here is to get constant folding done at the AST level so other, actually
 * interesting optimisations are not hindered by the non-foldedness of constants when they
 * subsequently are run.
 * Unfortunately, doing things at this level means we have to deal with issues like brackets.
 * TODO: Lambdas might be able to make this less soul-destroyingly awful to look at.
 */
public class ConstFoldTranslator extends TreeTranslator {
    private static Logger logger = LogManager.getLogger();

    // Boolean to track if this visitor has made any changes to the tree this iteration.
    private boolean mHasMadeAChange;

    /**
     *  Return if this visitor has made any changes to the tree.
     *  While this returns true, further passes are required to simplify everything.
     */
    public boolean makingChanges() {
        return mHasMadeAChange;
    }

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
        switch (nodeTag) {
            case POS:
                result = expr;
                logger.debug("+{} -> {}", expr, result);
                break;
            case NEG:
                result = numericalNegate((JCLiteral) expr);
                logger.debug("-{} -> {}", expr, result);
                break;
            case NOT:
                result = logicalNegate((JCLiteral) expr);
                logger.debug("!{} -> {}", expr, result);
                break;
            case COMPL:
                result = bitwiseNegate((JCLiteral) expr);
                logger.debug("~{} -> {}", expr, result);
                break;
            case PREINC:
                LogUtils.raiseCompilerError("Attempt to pre-inc a literal!");
                break;
            case PREDEC:
                LogUtils.raiseCompilerError("Attempt to pre-dec a literal!");
                break;
            case POSTINC:
                LogUtils.raiseCompilerError("Attempt to post-inc a literal!");
                break;
            case POSTDEC:
                LogUtils.raiseCompilerError("Attempt to post-dec a literal!");
                break;
        }
    }

    public void visitBinary(JCTree.JCBinary tree) {
        super.visitBinary(tree);
        // Determine the type of this unary operation.
        final Tag nodeTag = tree.getTag();
        JCExpression leftOperand = tree.getLeftOperand();
        JCExpression rightOperand = tree.getRightOperand();

        // Ensure this is an operation on literals before proceeding.
        if (!(leftOperand instanceof JCLiteral && rightOperand instanceof JCLiteral)) {
            return;
        }

        final Object leftPayload = ((JCLiteral) leftOperand).getValue();
        final Kind leftKind = leftOperand.getKind();

        final Object rightPayload = ((JCLiteral) rightOperand).getValue();
        final Kind rightKind = rightOperand.getKind();

        mHasMadeAChange = true;
        switch (nodeTag) {
            case BITOR:
                result = bitwiseOr(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} OR {} -> {}", leftOperand, rightOperand, result);
                break;
            case BITXOR:
                result = bitwiseXor(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} XOR {} -> {}", leftOperand, rightOperand, result);
                break;
            case BITAND:
                result = bitwiseAnd(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} AND {} -> {}", leftOperand, rightOperand, result);
                break;
            case SL:
                result = bitwiseLeftShift(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} << {} -> {}", leftOperand, rightOperand, result);
                break;
            case SR:
                result = bitwiseRightShift(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} >> {} -> {}", leftOperand, rightOperand, result);
                break;
            case USR:
                result = bitwiseUnsignedRightShift(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} >>> {} -> {}", leftOperand, rightOperand, result);
                break;
            case OR:
                result = logicalOr(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} || {} -> {}", leftOperand, rightOperand, result);
                break;
            case AND:
                result = logicalAnd(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} && {} -> {}", leftOperand, rightOperand, result);
                break;
            case EQ:
                result = logicalEq(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} == {} -> {}", leftOperand, rightOperand, result);
                break;
            case NE:
                result = logicalNeq(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} != {} -> {}", leftOperand, rightOperand, result);
                break;
            case LT:
                result = logicalLt(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} < {} -> {}", leftOperand, rightOperand, result);
                break;
            case GT:
                result = logicalGt(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} > {} -> {}", leftOperand, rightOperand, result);
                break;
            case LE:
                result = logicalLe(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} <= {} -> {}", leftOperand, rightOperand, result);
                break;
            case GE:
                result = logicalGe(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} >= {} -> {}", leftOperand, rightOperand, result);
                break;
            case PLUS:
                result = arithmeticPlus(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} + {} -> {}", leftOperand, rightOperand, result);
                break;
            case MINUS:
                result = arithmeticMinus(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} - {} -> {}", leftOperand, rightOperand, result);
                break;
            case MUL:
                result = arithmeticMultiply(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} * {} -> {}", leftOperand, rightOperand, result);
                break;
            case DIV:
                result = arithmeticDivide(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} / {} -> {}", leftOperand, rightOperand, result);
                break;
            case MOD:
                result = arithmeticModulo(leftPayload, leftKind, rightPayload, rightKind);
                logger.debug("{} % {} -> {}", leftOperand, rightOperand, result);
                break;
        }
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

    /**
     * Create a Literal tree node representing the bitwise OR of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand | rightOperand, or null in case of error.
     */
    private JCTree bitwiseOr(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-or non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue | longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue | longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue | longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the bitwise XOR of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand ^ rightOperand, or null in case of error.
     */
    private JCTree bitwiseXor(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-xor non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue ^ longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue ^ longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue ^ longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the bitwise AND of the two input Literal nodes.
     * 
     * @return A Literal node representing leftOperand & rightOperand, or null in case of error.
     */
    private JCTree bitwiseAnd(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-and non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue & longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue & longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue & longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the bitwise left shift of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand << rightOperand, or null in case of error.
     */
    private JCTree bitwiseLeftShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-left-shift non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue << longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue << longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue << longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the bitwise right shift of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand >> rightOperand, or null in case of error.
     */
    private JCTree bitwiseRightShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-right-shift non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue >> longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue >> longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue >> longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }


    /**
     * Create a Literal tree node representing the unsigned bitwise right shift of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand >>> rightOperand, or null in case of error.
     */
    private JCTree bitwiseUnsignedRightShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to binary-bitwise-unsigned-right-shift non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue >>> longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue >>> longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue >>> longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the logical OR of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand || rightOperand, or null in case of error.
     */
    private JCTree logicalOr(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are boolean literals.
        if (!(leftKind == Kind.BOOLEAN_LITERAL && rightKind == Kind.BOOLEAN_LITERAL)) {
            LogUtils.raiseCompilerError("Attempt to binary-logical-or non-boolean types: " + leftKind + ", " + rightKind);
            return null;
        }

        // Compute the logical or of the booleans and return the result.
        final boolean leftBool = (Boolean) leftPayload;
        final boolean rightBool = (Boolean) rightPayload;
        return treeMaker.Literal(leftBool || rightBool);
    }

    /**
     * Create a Literal tree node representing the logical AND of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand && rightOperand, or null in case of error.
     */
    private JCTree logicalAnd(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are boolean literals.
        if (!(leftKind == Kind.BOOLEAN_LITERAL && rightKind == Kind.BOOLEAN_LITERAL)) {
            LogUtils.raiseCompilerError("Attempt to binary-logical-or non-boolean types: " + leftKind + ", " + rightKind);
            return null;
        }

        // Compute the logical and of the booleans and return the result.
        final boolean leftBool = (Boolean) leftPayload;
        final boolean rightBool = (Boolean) rightPayload;
        return treeMaker.Literal(leftBool && rightBool);
    }

    /**
     * Create a Literal tree node representing the logical equals of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand == rightOperand, or null in case of error.
     */
    private JCTree logicalEq(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        if (rightKind != leftKind) {
            return treeMaker.Literal(false);
        }

        // It is not sufficient to check that leftPayload == rightPayload - we need to check if the
        // underlying primitive types are the same.
        // The behaviour in the case of strings is undefined, so let's not waste time comparing
        // contents. (Strings shouldn't be compared in this way anyway).
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                char charRValue = (Character) rightPayload;
                return treeMaker.Literal(charLValue == charRValue);
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                int intValue = (Integer) rightPayload;
                return treeMaker.Literal(intLValue == intValue);
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                long longRValue = (Long) rightPayload;
                return treeMaker.Literal(longLValue == longRValue);
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                float floatRValue = (Float) rightPayload;
                return treeMaker.Literal(floatLValue == floatRValue);
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                double doubleRValue = (Double) rightPayload;
                return treeMaker.Literal(doubleLValue == doubleRValue);
            case BOOLEAN_LITERAL:
                boolean boolLValue = (Boolean) leftPayload;
                boolean boolRValue = (Boolean) rightPayload;
                return treeMaker.Literal(boolLValue == boolRValue);
            case STRING_LITERAL:
                logger.warn("Comparing strings with == !");
                return treeMaker.Literal(false);
            case NULL_LITERAL:
                // Since null == null...
                return treeMaker.Literal(true);
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to logical-eq non-literal types " + leftKind + ", " + rightKind + " in ConstFolder.");
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the logical not-equals of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand != rightOperand, or null in case of error.
     */
    private JCTree logicalNeq(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        if (rightKind != leftKind) {
            return treeMaker.Literal(false);
        }

        // It is not sufficient to check that leftPayload != rightPayload - we need to check if the
        // underlying primitive types are the same.
        // The behaviour in the case of strings is undefined, so let's not waste time comparing
        // contents. (Strings shouldn't be compared in this way anyway).
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                char charRValue = (Character) rightPayload;
                return treeMaker.Literal(charLValue != charRValue);
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                int intValue = (Integer) rightPayload;
                return treeMaker.Literal(intLValue != intValue);
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                long longRValue = (Long) rightPayload;
                return treeMaker.Literal(longLValue != longRValue);
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                float floatRValue = (Float) rightPayload;
                return treeMaker.Literal(floatLValue != floatRValue);
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                double doubleRValue = (Double) rightPayload;
                return treeMaker.Literal(doubleLValue != doubleRValue);
            case BOOLEAN_LITERAL:
                boolean boolLValue = (Boolean) leftPayload;
                boolean boolRValue = (Boolean) rightPayload;
                return treeMaker.Literal(boolLValue != boolRValue);
            case STRING_LITERAL:
                logger.warn("Comparing strings with == !");
                return treeMaker.Literal(true);
            case NULL_LITERAL:
                // Since null != null is false...
                return treeMaker.Literal(false);
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to logical-neq non-literal types " + leftKind + ", " + rightKind + " in ConstFolder.");
                return null;
        }
    }

    /**
     * Create a Literal tree node representing l < r for two input Literal nodes.
     *
     * @return A Literal node representing leftOperand < rightOperand, or null in case of error.
     */
    private JCTree logicalLt(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to lt non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue < floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue < floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue < floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue < floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue < floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing l > r for two input Literal nodes.
     *
     * @return A Literal node representing leftOperand > rightOperand, or null in case of error.
     */
    private JCTree logicalGt(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to gt non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue > floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue > floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue > floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue > floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue > floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing l <= r for two input Literal nodes.
     *
     * @return A Literal node representing leftOperand <= rightOperand, or null in case of error.
     */
    private JCTree logicalLe(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to le non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue <= floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue <= floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue <= floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue <= floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue <= floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing l >= r from the operand nodes.
     *
     * @return A Literal node representing leftOperand >= rightOperand, or null in case of error.
     */
    private JCTree logicalGe(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to ge non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue >= floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue >= floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue >= floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue >= floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue >= floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the sum of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand + rightOperand, or null in case of error.
     */
    private JCTree arithmeticPlus(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to add non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue + stringRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue + stringRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue + stringRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue + stringRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue + stringRValue);
                    default:
                        return null;
                }
            case STRING_LITERAL:
                String stringLValue = (String) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(stringLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(stringLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(stringLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(stringLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(stringLValue + floatRValue);
                    case STRING_LITERAL:
                        double stringRValue = (Float) rightPayload;
                        return treeMaker.Literal(stringLValue + stringRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the difference of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand - rightOperand, or null in case of error.
     */
    private JCTree arithmeticMinus(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to subtract non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue - floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue - floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue - floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue - floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue - floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the product of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand * rightOperand, or null in case of error.
     */
    private JCTree arithmeticMultiply(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to multiply non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue * floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue * floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue * floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue * floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue * floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the quotient of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand / rightOperand, or null in case of error.
     */
    private JCTree arithmeticDivide(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to divide non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue / floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue / floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue / floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue / floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue / floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Create a Literal tree node representing the modulo of the two input Literal nodes.
     *
     * @return A Literal node representing leftOperand % rightOperand, or null in case of error.
     */
    private JCTree arithmeticModulo(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are numeric literals.
        if (!kindIsNumeric(leftKind) || !kindIsNumeric(rightKind)) {
            LogUtils.raiseCompilerError("Attempt to modulo non-numeric types: " + leftKind + ", " + rightKind);
            return null;
        }

        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(charLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(charLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(charLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(charLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(charLValue % floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(intLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(intLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(intLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(intLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(intLValue % floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(longLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(longLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(longLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(longLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(longLValue % floatRValue);
                    default:
                        return null;
                }

            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(doubleLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(doubleLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(doubleLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(doubleLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(doubleLValue % floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return treeMaker.Literal(floatLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return treeMaker.Literal(floatLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return treeMaker.Literal(floatLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return treeMaker.Literal(floatLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return treeMaker.Literal(floatLValue % floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Helper method to return a Literal node that is the logical negation of the input.
     *
     * @param expr JCLiteral representing the literal to be negated.
     * @return A JCLiteral that is the logical negation of the input.
     */
    private JCTree logicalNegate(JCLiteral expr) {
        // Extract the value and type from the literal.
        final Object literalPayload = expr.getValue();
        final Kind literalKind = expr.getKind();

        if (literalKind != Kind.BOOLEAN_LITERAL) {
            LogUtils.raiseCompilerError("Attempt to unary-boolean-negate non-boolean type: " + literalKind);
            return null;
        }

        boolean boolValue = (Boolean) literalPayload;
        return treeMaker.Literal(!boolValue);
    }

    private JCTree bitwiseNegate(JCLiteral expr) {
        // Extract the value and type from the literal.
        final Object literalPayload = expr.getValue();
        final Kind literalKind = expr.getKind();

        switch (literalKind) {
            case CHAR_LITERAL:
                char charValue = (Character) literalPayload;
                return treeMaker.Literal(~charValue);
            case INT_LITERAL:
                int intValue = (Integer) literalPayload;
                return treeMaker.Literal(~intValue);
            case LONG_LITERAL:
                long longValue = (Long) literalPayload;
                return treeMaker.Literal(~longValue);
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                LogUtils.raiseCompilerError("Attempt to unary-bitwise-negate invalid type: " + literalKind);
                break;
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to unary-bitwise-negate non-literal type " + literalKind + " in ConstFolder.");
                break;
        }

        return null;
    }

    /**
     * Helper method to return a Literal node that is the numerical negation of the input.
     *
     * @param expr JCLiteral representing the literal to be negated.
     * @return A JCLiteral that is the negation of the input.
     */
    private JCTree numericalNegate(JCLiteral expr) {
        // Extract the value and type from the literal.
        final Object literalPayload = expr.getValue();
        final Kind literalKind = expr.getKind();

        switch (literalKind) {
            case CHAR_LITERAL:
                char charValue = (Character) literalPayload;
                return treeMaker.Literal(-charValue);
            case INT_LITERAL:
                int intValue = (Integer) literalPayload;
                return treeMaker.Literal(-intValue);
            case LONG_LITERAL:
                long longValue = (Long) literalPayload;
                return treeMaker.Literal(-longValue);
            case FLOAT_LITERAL:
                float floatValue = (Float) literalPayload;
                return treeMaker.Literal(-floatValue);
            case DOUBLE_LITERAL:
                double doubleValue = (Double) literalPayload;
                return treeMaker.Literal(-doubleValue);
            case BOOLEAN_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                LogUtils.raiseCompilerError("Attempt to unary-negate invalid type: " + literalKind);
                break;
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to unary-negate non-literal type " + literalKind + " in ConstFolder.");
                break;
        }

        return null;
    }

    /**
     * Check if a given Kind is a numeric type of not.
     *
     * @param k Kind to check.
     * @return True if the kind provided is of a numeric type, false otherwise.
     */
    private boolean kindIsNumeric(Kind k) {
        return k == Kind.LONG_LITERAL ||
               k == Kind.INT_LITERAL  ||
               k == Kind.CHAR_LITERAL  ||
               k == Kind.FLOAT_LITERAL  ||
               k == Kind.DOUBLE_LITERAL;
    }
}
