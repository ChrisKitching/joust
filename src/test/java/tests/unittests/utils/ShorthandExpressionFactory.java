package tests.unittests.utils;

import com.sun.tools.javac.code.TypeTag;

import static joust.utils.compiler.StaticCompilerUtils.treeMaker;
import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.code.Symbol.*;

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

    public static AJCBinary plus(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.PLUS, lValue, rValue);
    }

    public static AJCBinary minus(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.MINUS, lValue, rValue);
    }

    public static AJCBinary mul(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.MUL, lValue, rValue);
    }

    public static AJCBinary div(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.DIV, lValue, rValue);
    }

    public static AJCBinary mod(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.MOD, lValue, rValue);
    }

    public static AJCBinary and(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.AND, lValue, rValue);
    }

    public static AJCBinary or(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.OR, lValue, rValue);
    }

    public static AJCBinary bitAnd(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.BITAND, lValue, rValue);
    }

    public static AJCBinary bitOr(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.BITOR, lValue, rValue);
    }

    public static AJCBinary bitXor(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.BITXOR, lValue, rValue);
    }

    public static AJCBinary lShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.SL, lValue, rValue);
    }

    public static AJCBinary rShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.SR, lValue, rValue);
    }

    public static AJCBinary urShift(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.USR, lValue, rValue);
    }

    public static AJCBinary eq(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.EQ, lValue, rValue);
    }

    public static AJCBinary neq(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.NE, lValue, rValue);
    }

    public static AJCBinary lt(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.LT, lValue, rValue);
    }

    public static AJCBinary gt(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.GT, lValue, rValue);
    }

    public static AJCBinary le(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.LE, lValue, rValue);
    }

    public static AJCBinary ge(AJCExpressionTree lValue, AJCExpressionTree rValue) {
        return treeMaker.Binary(Tag.GE, lValue, rValue);
    }

    /*
     * Unary operators...
     */

    public static AJCUnary pos(AJCExpressionTree lValue) {
        return treeMaker.Unary(Tag.POS, lValue);
    }

    public static AJCUnary neg(AJCExpressionTree lValue) {
        return treeMaker.Unary(Tag.NEG, lValue);
    }

    public static AJCUnary not(AJCExpressionTree lValue) {
        return treeMaker.Unary(Tag.NOT, lValue);
    }

    public static AJCUnary comp(AJCExpressionTree lValue) {
        return treeMaker.Unary(Tag.COMPL, lValue);
    }


    public static AJCUnaryAsg preInc(AJCSymbolRefTree<VarSymbol> lValue) {
        return treeMaker.UnaryAsg(Tag.PREINC, lValue);
    }

    public static AJCUnaryAsg postInc(AJCSymbolRefTree<VarSymbol> lValue) {
        return treeMaker.UnaryAsg(Tag.POSTINC, lValue);
    }

    public static AJCUnaryAsg preDec(AJCSymbolRefTree<VarSymbol> lValue) {
        return treeMaker.UnaryAsg(Tag.PREDEC, lValue);
    }

    public static AJCUnaryAsg postDec(AJCSymbolRefTree<VarSymbol> lValue) {
        return treeMaker.UnaryAsg(Tag.POSTDEC, lValue);
    }
}
