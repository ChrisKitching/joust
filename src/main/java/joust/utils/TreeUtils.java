package joust.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A collection of utility functions for handling JCTree nodes.
 */
public @Log4j2
class TreeUtils {
    public static boolean isLocalVariable(Symbol sym) {
        return sym.owner instanceof MethodSymbol;
    }
    public static boolean isLocalVariable(JCIdent ident) {
        return ident.sym instanceof MethodSymbol;
    }

    // Helps simplify the annoying situations where JCTree nodes give us JCExpressions that are
    // always idents and suchlike.
    public static boolean isLocalVariable(JCTree tree) {
        return isLocalVariable((JCIdent) tree);
    }

    public static VarSymbol getTargetSymbolForExpression(JCExpression that) {
        if (that instanceof JCFieldAccess) {
            JCFieldAccess cast = (JCFieldAccess) that;
            while (!(cast.selected instanceof JCIdent)) {
                if (cast.selected instanceof JCFieldAccess) {
                    cast = (JCFieldAccess) cast.selected;
                } else {
                    log.warn("Unable to find target symbol for {}\nStuck with cast = {}, next {} of type {}", that, cast, cast.selected, cast.selected.getClass().getSimpleName());
                    return null;
                }
            }

            return (VarSymbol) ((JCIdent) cast.selected).sym;
        } else if (that instanceof JCIdent) {
            return (VarSymbol) ((JCIdent) that).sym;
        } else {
            LogUtils.raiseCompilerError("Unexpected expression target type: " + that.getClass().getCanonicalName() + " for node: " + that);
        }

        return null;
    }

    public static VarSymbol getTargetSymbolForAssignment(JCAssignOp that) {
        return getTargetSymbolForExpression(that.lhs);
    }

    public static VarSymbol getTargetSymbolForAssignment(JCAssign that) {
        return getTargetSymbolForExpression(that.lhs);
    }

    public static boolean operatorIsCommutative(Tag opcode) {
        return opcode == Tag.BITOR
            || opcode == Tag.BITXOR
            || opcode == Tag.BITAND
            || opcode == Tag.OR
            || opcode == Tag.AND
            || opcode == Tag.EQ
            || opcode == Tag.NE
            || opcode == Tag.PLUS
            || opcode == Tag.MUL;
    }
}
