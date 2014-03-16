package tests.unittests;

import com.sun.tools.javac.code.TypeTag;

import static joust.utils.StaticCompilerUtils.treeMaker;
import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;

/**
 * Provides a collection of methods for conveniently creating literal trees for tests.
 */
public final class ShorthandExpressionFactory {
    public static AJCLiteral l(Object o) {
        // Workaround for bug 9008072 in OpenJDK...
        if (o instanceof Character) {
            // Yes. That's actually how javac does the conversion.
            return treeMaker.Literal(TypeTag.CHAR, (int) o.toString().charAt(0));
        }
        return treeMaker.Literal(o);
    }

    /*
     * Binary operators...
     */

    public static AJCBinary plus(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.PLUS, lValue, rValue);
    }

    public static AJCBinary minus(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.MINUS, lValue, rValue);
    }

    public static AJCBinary mul(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.MUL, lValue, rValue);
    }

    public static AJCBinary div(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.DIV, lValue, rValue);
    }

    public static AJCBinary mod(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.MOD, lValue, rValue);
    }

    public static AJCBinary and(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.AND, lValue, rValue);
    }

    public static AJCBinary or(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.OR, lValue, rValue);
    }

    public static AJCBinary bitAnd(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.BITAND, lValue, rValue);
    }

    public static AJCBinary bitOr(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.BITOR, lValue, rValue);
    }

    public static AJCBinary bitXor(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.BITXOR, lValue, rValue);
    }

    public static AJCBinary lShift(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.SL, lValue, rValue);
    }

    public static AJCBinary rShift(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.SR, lValue, rValue);
    }

    public static AJCBinary urShift(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.USR, lValue, rValue);
    }

    public static AJCBinary eq(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.EQ, lValue, rValue);
    }

    public static AJCBinary neq(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.NE, lValue, rValue);
    }

    public static AJCBinary lt(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.LT, lValue, rValue);
    }

    public static AJCBinary gt(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.GT, lValue, rValue);
    }

    public static AJCBinary le(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.LE, lValue, rValue);
    }

    public static AJCBinary ge(AJCExpression lValue, AJCExpression rValue) {
        return treeMaker.Binary(Tag.GE, lValue, rValue);
    }

    /*
     * Unary operators...
     */

    public static AJCUnary pos(AJCExpression lValue) {
        return treeMaker.Unary(Tag.POS, lValue);
    }

    public static AJCUnary neg(AJCExpression lValue) {
        return treeMaker.Unary(Tag.NEG, lValue);
    }

    public static AJCUnary not(AJCExpression lValue) {
        return treeMaker.Unary(Tag.NOT, lValue);
    }

    public static AJCUnary comp(AJCExpression lValue) {
        return treeMaker.Unary(Tag.COMPL, lValue);
    }
}
