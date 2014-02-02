package joust.optimisers.evaluation;

import com.sun.tools.javac.code.TypeTag;
import joust.utils.LogUtils;
import lombok.extern.log4j.Log4j2;


import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.Optimiser.treeMaker;

/**
 * Represents the current value of a variable in an EvaluationContext.
 */
public @Log4j2
class Value {
    // The "I have no fucking idea" value.
    public static final Value UNKNOWN = new Value();

    // The actual value of this object, if we know what it is. We use the same representation as JCLiteral.
    private Object value;

    public TypeTag typetag;

    /**
     * Stolen from JCLiteral...
     */
    public Object getValue() {
        switch (typetag) {
            case BOOLEAN:
                if (value instanceof Integer) {
                    int bi = (Integer) value;
                    return (bi != 0);
                }
                return value;
            case CHAR:
                if (value instanceof Integer) {
                    int ci = (Integer) value;
                    char c = (char) ci;
                    if (c != ci) {
                        throw new AssertionError("bad value for char value");
                    }
                    return c;
                }

                return value;
            default:
                return value;
        }
    }

    public Kind getKind() {
        return typetag.getKindLiteral();
    }

    // Operators. So many operators.
    public static Value unary(Tag opcode, Value operand) {
        if (operand == UNKNOWN) {
            return UNKNOWN;
        }

        final Object payload = operand.getValue();
        final Kind kind = operand.getKind();

        Value result = UNKNOWN;
        switch (opcode) {
            case POS:
                result = operand;
                log.debug("+{} -> {}", operand, result);
                break;
            case NEG:
                result = numericalNegate(payload, kind);
                log.debug("-{} -> {}", operand, result);
                break;
            case NOT:
                result = logicalNegate(payload, kind);
                log.debug("!{} -> {}", operand, result);
                break;
            case COMPL:
                result = bitwiseNegate(payload, kind);
                log.debug("~{} -> {}", operand, result);
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

        return result;
    }

    public static Value binary(Tag opcode, Value leftOperand, Value rightOperand) {
        if (leftOperand == UNKNOWN || rightOperand == UNKNOWN) {
            return UNKNOWN;
        }

        final Object leftPayload = leftOperand.getValue();
        final Kind leftKind = leftOperand.getKind();

        final Object rightPayload = rightOperand.getValue();
        final Kind rightKind = rightOperand.getKind();

        Value result;
        switch (opcode) {
            case BITOR:
                result = bitwiseOr(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} OR {} -> {}", leftOperand, rightOperand, result);
                break;
            case BITXOR:
                result = bitwiseXor(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} XOR {} -> {}", leftOperand, rightOperand, result);
                break;
            case BITAND:
                result = bitwiseAnd(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} AND {} -> {}", leftOperand, rightOperand, result);
                break;
            case SL:
                result = bitwiseLeftShift(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} << {} -> {}", leftOperand, rightOperand, result);
                break;
            case SR:
                result = bitwiseRightShift(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} >> {} -> {}", leftOperand, rightOperand, result);
                break;
            case USR:
                result = bitwiseUnsignedRightShift(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} >>> {} -> {}", leftOperand, rightOperand, result);
                break;
            case OR:
                result = logicalOr(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} || {} -> {}", leftOperand, rightOperand, result);
                break;
            case AND:
                result = logicalAnd(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} && {} -> {}", leftOperand, rightOperand, result);
                break;
            case EQ:
                result = logicalEq(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} == {} -> {}", leftOperand, rightOperand, result);
                break;
            case NE:
                result = logicalNeq(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} != {} -> {}", leftOperand, rightOperand, result);
                break;
            case LT:
                result = logicalLt(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} < {} -> {}", leftOperand, rightOperand, result);
                break;
            case GT:
                result = logicalGt(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} > {} -> {}", leftOperand, rightOperand, result);
                break;
            case LE:
                result = logicalLe(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} <= {} -> {}", leftOperand, rightOperand, result);
                break;
            case GE:
                result = logicalGe(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} >= {} -> {}", leftOperand, rightOperand, result);
                break;
            case PLUS:
                result = arithmeticPlus(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} + {} -> {}", leftOperand, rightOperand, result);
                break;
            case MINUS:
                result = arithmeticMinus(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} - {} -> {}", leftOperand, rightOperand, result);
                break;
            case MUL:
                result = arithmeticMultiply(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} * {} -> {}", leftOperand, rightOperand, result);
                break;
            case DIV:
                result = arithmeticDivide(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} / {} -> {}", leftOperand, rightOperand, result);
                break;
            case MOD:
                result = arithmeticModulo(leftPayload, leftKind, rightPayload, rightKind);
                log.debug("{} % {} -> {}", leftOperand, rightOperand, result);
                break;
            default:
                result = UNKNOWN;
                break;
        }

        return result;
    }

    private static Value bitwiseOr(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue | longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue | longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue | charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue | intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue | longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value bitwiseXor(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue ^ longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue ^ longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue ^ charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue ^ intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue ^ longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value bitwiseAnd(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue & longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue & longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue & charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue & intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue & longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value bitwiseLeftShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue << longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue << longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue << charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue << intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue << longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value bitwiseRightShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue >> longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue >> longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue >> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue >> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue >> longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value bitwiseUnsignedRightShift(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue >>> longRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue >>> longRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue >>> charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue >>> intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue >>> longRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value logicalOr(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are boolean literals.
        if (!(leftKind == Kind.BOOLEAN_LITERAL && rightKind == Kind.BOOLEAN_LITERAL)) {
            LogUtils.raiseCompilerError("Attempt to binary-logical-or non-boolean types: " + leftKind + ", " + rightKind);
            return null;
        }

        // Compute the logical or of the booleans and return the result.
        final boolean leftBool = (Boolean) leftPayload;
        final boolean rightBool = (Boolean) rightPayload;
        return Value.of(leftBool || rightBool);
    }

    private static Value logicalAnd(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // Verify the types of the operands are boolean literals.
        if (!(leftKind == Kind.BOOLEAN_LITERAL && rightKind == Kind.BOOLEAN_LITERAL)) {
            LogUtils.raiseCompilerError("Attempt to binary-logical-or non-boolean types: " + leftKind + ", " + rightKind);
            return null;
        }

        // Compute the logical and of the booleans and return the result.
        final boolean leftBool = (Boolean) leftPayload;
        final boolean rightBool = (Boolean) rightPayload;
        return Value.of(leftBool && rightBool);
    }

    private static Value logicalEq(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        if (rightKind != leftKind) {
            return Value.of(false);
        }

        // It is not sufficient to check that leftPayload == rightPayload - we need to check if the
        // underlying primitive types are the same.
        // The behaviour in the case of strings is undefined, so let's not waste time comparing
        // contents. (Strings shouldn't be compared in this way anyway).
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                char charRValue = (Character) rightPayload;
                return Value.of(charLValue == charRValue);
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                int intValue = (Integer) rightPayload;
                return Value.of(intLValue == intValue);
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                long longRValue = (Long) rightPayload;
                return Value.of(longLValue == longRValue);
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                float floatRValue = (Float) rightPayload;
                return Value.of(floatLValue == floatRValue);
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                double doubleRValue = (Double) rightPayload;
                return Value.of(doubleLValue == doubleRValue);
            case BOOLEAN_LITERAL:
                boolean boolLValue = (Boolean) leftPayload;
                boolean boolRValue = (Boolean) rightPayload;
                return Value.of(boolLValue == boolRValue);
            case STRING_LITERAL:
                log.warn("Comparing strings with == !");
                return Value.of(false);
            case NULL_LITERAL:
                // Since null == null...
                return Value.of(true);
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to logical-eq non-literal types " + leftKind + ", " + rightKind + " in ConstFolder.");
                return null;
        }
    }

    private static Value logicalNeq(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        if (rightKind != leftKind) {
            return Value.of(false);
        }

        // It is not sufficient to check that leftPayload != rightPayload - we need to check if the
        // underlying primitive types are the same.
        // The behaviour in the case of strings is undefined, so let's not waste time comparing
        // contents. (Strings shouldn't be compared in this way anyway).
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                char charRValue = (Character) rightPayload;
                return Value.of(charLValue != charRValue);
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                int intValue = (Integer) rightPayload;
                return Value.of(intLValue != intValue);
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                long longRValue = (Long) rightPayload;
                return Value.of(longLValue != longRValue);
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                float floatRValue = (Float) rightPayload;
                return Value.of(floatLValue != floatRValue);
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                double doubleRValue = (Double) rightPayload;
                return Value.of(doubleLValue != doubleRValue);
            case BOOLEAN_LITERAL:
                boolean boolLValue = (Boolean) leftPayload;
                boolean boolRValue = (Boolean) rightPayload;
                return Value.of(boolLValue != boolRValue);
            case STRING_LITERAL:
                log.warn("Comparing strings with == !");
                return Value.of(true);
            case NULL_LITERAL:
                // Since null != null is false...
                return Value.of(false);
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to logical-neq non-literal types " + leftKind + ", " + rightKind + " in ConstFolder.");
                return null;
        }
    }

    private static Value logicalLt(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue < floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue < floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue < floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue < floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue < charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue < intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue < longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue < doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue < floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value logicalGt(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue > floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue > floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue > floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue > floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue > charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue > intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue > longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue > doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue > floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value logicalLe(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue <= floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue <= floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue <= floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue <= floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue <= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue <= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue <= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue <= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue <= floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value logicalGe(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue >= floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue >= floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue >= floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue >= floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue >= charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue >= intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue >= longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue >= doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue >= floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value arithmeticPlus(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
        // For each of the nine possible cases, cast the payloads to their real types and find the result.
        switch (leftKind) {
            case CHAR_LITERAL:
                char charLValue = (Character) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(charLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(charLValue + stringRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(Integer.toString(intLValue) + stringRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(Long.toString(longLValue) + stringRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(Double.toString(doubleLValue) + stringRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(Float.toString(floatLValue) + stringRValue);
                    default:
                        return null;
                }
            case STRING_LITERAL:
                String stringLValue = (String) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(stringLValue + charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(stringLValue + intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(stringLValue + longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(stringLValue + doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(stringLValue + floatRValue);
                    case STRING_LITERAL:
                        String stringRValue = (String) rightPayload;
                        return Value.of(stringLValue + stringRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value arithmeticMinus(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue - floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue - floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue - floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue - floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue - charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue - intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue - longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue - doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue - floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value arithmeticMultiply(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue * floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue * floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue * floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue * floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue * charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue * intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue * longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue * doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue * floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value arithmeticDivide(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue / floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue / floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue / floatRValue);
                    default:
                        return null;
                }
            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue / floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue / charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue / intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue / longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue / doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue / floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value arithmeticModulo(Object leftPayload, Kind leftKind, Object rightPayload, Kind rightKind) {
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
                        return Value.of(charLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(charLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(charLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(charLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(charLValue % floatRValue);
                    default:
                        return null;
                }
            case INT_LITERAL:
                int intLValue = (Integer) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(intLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(intLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(intLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(intLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(intLValue % floatRValue);
                    default:
                        return null;
                }
            case LONG_LITERAL:
                long longLValue = (Long) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(longLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(longLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(longLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(longLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(longLValue % floatRValue);
                    default:
                        return null;
                }

            case DOUBLE_LITERAL:
                double doubleLValue = (Double) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(doubleLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(doubleLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(doubleLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(doubleLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(doubleLValue % floatRValue);
                    default:
                        return null;
                }
            case FLOAT_LITERAL:
                float floatLValue = (Float) leftPayload;
                switch (rightKind) {
                    case CHAR_LITERAL:
                        char charRValue = (Character) rightPayload;
                        return Value.of(floatLValue % charRValue);
                    case INT_LITERAL:
                        int intRValue = (Integer) rightPayload;
                        return Value.of(floatLValue % intRValue);
                    case LONG_LITERAL:
                        long longRValue = (Long) rightPayload;
                        return Value.of(floatLValue % longRValue);
                    case DOUBLE_LITERAL:
                        double doubleRValue = (Double) rightPayload;
                        return Value.of(floatLValue % doubleRValue);
                    case FLOAT_LITERAL:
                        float floatRValue = (Float) rightPayload;
                        return Value.of(floatLValue % floatRValue);
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private static Value logicalNegate(Object payload, Kind kind) {
        if (kind != Kind.BOOLEAN_LITERAL) {
            LogUtils.raiseCompilerError("Attempt to unary-boolean-negate non-boolean type: " + kind);
            return null;
        }

        boolean boolValue = (Boolean) payload;
        return Value.of(!boolValue);
    }

    private static Value bitwiseNegate(Object payload, Kind kind) {
        switch (kind) {
            case CHAR_LITERAL:
                char charValue = (Character) payload;
                return Value.of(~charValue);
            case INT_LITERAL:
                int intValue = (Integer) payload;
                return Value.of(~intValue);
            case LONG_LITERAL:
                long longValue = (Long) payload;
                return Value.of(~longValue);
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case BOOLEAN_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                LogUtils.raiseCompilerError("Attempt to unary-bitwise-negate invalid type: " + kind);
                break;
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to unary-bitwise-negate non-literal type " + kind + " in ConstFolder.");
                break;
        }

        return null;
    }

    private static Value numericalNegate(Object payload, Kind kind) {
        switch (kind) {
            case CHAR_LITERAL:
                char charValue = (Character) payload;
                return Value.of(-charValue);
            case INT_LITERAL:
                int intValue = (Integer) payload;
                return Value.of(-intValue);
            case LONG_LITERAL:
                long longValue = (Long) payload;
                return Value.of(-longValue);
            case FLOAT_LITERAL:
                float floatValue = (Float) payload;
                return Value.of(-floatValue);
            case DOUBLE_LITERAL:
                double doubleValue = (Double) payload;
                return Value.of(-doubleValue);
            case BOOLEAN_LITERAL:
            case STRING_LITERAL:
            case NULL_LITERAL:
                LogUtils.raiseCompilerError("Attempt to unary-negate invalid type: " + kind);
                break;
            default:
                LogUtils.raiseCompilerError("[BUG] Attempt to unary-negate non-literal type " + kind + " in ConstFolder.");
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
    private static boolean kindIsNumeric(Kind k) {
        return k == Kind.LONG_LITERAL
            || k == Kind.INT_LITERAL
            || k == Kind.CHAR_LITERAL
            || k == Kind.FLOAT_LITERAL
            || k == Kind.DOUBLE_LITERAL;
    }

    // Constructors for every primitive type...
    private Value(byte x) {
        value = x;
        typetag = TypeTag.BYTE;
    }

    private Value(short x) {
        value = x;
        typetag = TypeTag.SHORT;
    }

    private Value(char x) {
        value = x;
        typetag = TypeTag.CHAR;
    }

    private Value(int x) {
        value = x;
        typetag = TypeTag.INT;
    }

    private Value(long x) {
        value = x;
        typetag = TypeTag.LONG;
    }

    private Value(float x) {
        value = x;
        typetag = TypeTag.FLOAT;
    }

    private Value(double x) {
        value = x;
        typetag = TypeTag.DOUBLE;
    }

    private Value(boolean x) {
        value = x;
        typetag = TypeTag.BOOLEAN;
    }

    private Value(String x) {
        value = x;
        // Bewilderingly, this maps to Kind.STRING_LITERAL *and nothing else*.
        typetag = TypeTag.CLASS;
    }

    public Value() {
        typetag = TypeTag.UNKNOWN;
    }

    public static Value of(Object value) {
        // Urgh.
        if (value instanceof Byte) {
            return new Value((Byte) value);
        } else if (value instanceof Character) {
            return new Value((Character) value);
        } else if (value instanceof Short) {
            return new Value((Short) value);
        } else if (value instanceof Integer) {
            return new Value((Integer) value);
        } else if (value instanceof Long) {
            return new Value((Long) value);
        } else if (value instanceof Float) {
            return new Value((Float) value);
        } else if (value instanceof Double) {
            return new Value((Double) value);
        } else if (value instanceof Boolean) {
            return new Value((Boolean) value);
        } else if (value instanceof String) {
            return new Value((String) value);
        } else {
            return UNKNOWN;
        }
    }

    public JCLiteral toLiteral() {
        if (this == UNKNOWN) {
            LogUtils.raiseCompilerError("Attempt to literalify UNKNOWN value!");
            return null;
        }

        return treeMaker.Literal(value);
    }

    @Override
    public String toString() {
        return this == UNKNOWN ? "UNKNOWN" : value.toString();
    }
}
