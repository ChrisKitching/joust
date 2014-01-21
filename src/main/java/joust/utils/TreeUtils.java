package joust.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A collection of utility functions for handling JCTree nodes.
 */
public class TreeUtils {
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
            return (VarSymbol) ((JCFieldAccess) that).sym;
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
}
