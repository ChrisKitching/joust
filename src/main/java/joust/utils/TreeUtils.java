package joust.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A collection of utility functions for handling JCTree nodes.
 */
public class TreeUtils {
    public static boolean isLocalVariable(Symbol sym) {
        return sym.owner instanceof Symbol.MethodSymbol;
    }
    public static boolean isLocalVariable(JCIdent ident) {
        return ident.sym instanceof Symbol.MethodSymbol;
    }

    // Helps simplify the annoying situations where JCTree nodes give us JCExpressions that are
    // always idents and suchlike.
    public static boolean isLocalVariable(JCTree tree) {
        return isLocalVariable((JCIdent) tree);
    }
}
