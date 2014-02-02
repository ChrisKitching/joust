package tests.unittests;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.code.TypeTag;

import static tests.unittests.TreeFabricatingTest.t;

/**
 * Base class for tests of constant folding. Provides a collection of methods for conveniently
 * creating literal trees.
 */
public class ExpressionFactory {
    public static JCParens parens(JCExpression e) {
        return t.Parens(e);
    }

    public static JCLiteral l(Object o) {
        // Workaround for bug 9008072 in OpenJDK...
        if (o instanceof Character) {
            // Yes. That's actually how javac does the conversion.
            return t.Literal(TypeTag.CHAR, (int) ((o).toString().charAt(0)));
        }
        return t.Literal(o);
    }

    /*
     * Binary operators...
     */

    public static JCBinary plus(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.PLUS, lValue, rValue);
    }

    public static JCBinary minus(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.MINUS, lValue, rValue);
    }

    public static JCBinary mul(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.MUL, lValue, rValue);
    }

    public static JCBinary div(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.DIV, lValue, rValue);
    }

    public static JCBinary mod(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.MOD, lValue, rValue);
    }

    public static JCBinary and(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.AND, lValue, rValue);
    }

    public static JCBinary or(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.OR, lValue, rValue);
    }

    public static JCBinary bitAnd(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.BITAND, lValue, rValue);
    }

    public static JCBinary bitOr(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.BITOR, lValue, rValue);
    }

    public static JCBinary bitXor(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.BITXOR, lValue, rValue);
    }

    public static JCBinary lShift(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.SL, lValue, rValue);
    }

    public static JCBinary rShift(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.SR, lValue, rValue);
    }

    public static JCBinary urShift(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.USR, lValue, rValue);
    }

    public static JCBinary eq(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.EQ, lValue, rValue);
    }

    public static JCBinary neq(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.NE, lValue, rValue);
    }

    public static JCBinary lt(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.LT, lValue, rValue);
    }

    public static JCBinary gt(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.GT, lValue, rValue);
    }

    public static JCBinary le(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.LE, lValue, rValue);
    }

    public static JCBinary ge(JCExpression lValue, JCExpression rValue) {
        return t.Binary(Tag.GE, lValue, rValue);
    }

    /*
     * Unary operators...
     */

    public static JCUnary pos(JCExpression lValue) {
        return t.Unary(Tag.POS, lValue);
    }

    public static JCUnary neg(JCExpression lValue) {
        return t.Unary(Tag.NEG, lValue);
    }

    public static JCUnary not(JCExpression lValue) {
        return t.Unary(Tag.NOT, lValue);
    }

    public static JCUnary comp(JCExpression lValue) {
        return t.Unary(Tag.COMPL, lValue);
    }
}
