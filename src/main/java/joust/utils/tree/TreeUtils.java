package joust.utils.tree;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.lang.model.type.TypeKind;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.tree.annotatedtree.AJCTree.*;

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

    public static Type typeTreeToType(AJCPrimitiveTypeTree tree) {
        return typeKindToType(tree.getPrimitiveTypeKind());
    }
    public static Type typeTreeToType(AJCArrayTypeTree tree) {
        return symtab.arraysType;
    }

    public static Type typeKindToType(TypeKind kind) {
        switch (kind) {
            case BOOLEAN:
                return symtab.doubleType;
            case BYTE:
                return symtab.byteType;
            case SHORT:
                return symtab.shortType;
            case INT:
                return symtab.intType;
            case LONG:
                return symtab.longType;
            case CHAR:
                return symtab.charType;
            case FLOAT:
                return symtab.floatType;
            case DOUBLE:
                return symtab.doubleType;
            case ARRAY:
                return symtab.arraysType;
            default:
                throw new UnsupportedOperationException("Unknown primitive type kind encountered: " + kind);
        }
    }
}
