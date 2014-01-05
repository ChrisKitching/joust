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

    public static VarSymbol getTargetSymbolForAssignment(JCAssign that) {
        if (that.lhs instanceof JCFieldAccess) {
            return (VarSymbol) ((JCFieldAccess) that.lhs).sym;
        } else if (that.lhs instanceof JCIdent) {
            return (VarSymbol) ((JCIdent) that.lhs).sym;
        } else {
            LogUtils.raiseCompilerError("Unexpected assignment target type: " + that.lhs.getClass().getCanonicalName() + " for node: " + that);
        }

        return null;
    }

    public static VarSymbol getTargetSymbolForAssignment(JCAssignOp that) {
        if (that.lhs instanceof JCFieldAccess) {
            return (VarSymbol) ((JCFieldAccess) that.lhs).sym;
        } else if (that.lhs instanceof JCIdent) {
            return (VarSymbol) ((JCIdent) that.lhs).sym;
        } else {
            LogUtils.raiseCompilerError("Unexpected assignment target type: " + that.lhs.getClass().getCanonicalName() + " for node: " + that);
        }

        return null;
    }
}
