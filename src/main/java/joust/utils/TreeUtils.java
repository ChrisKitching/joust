package joust.utils;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A collection of utility functions for handling JCTree nodes.
 */
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@Log
public class TreeUtils {
    public static boolean isLocalVariable(Symbol sym) {
        return sym.owner instanceof MethodSymbol;
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
