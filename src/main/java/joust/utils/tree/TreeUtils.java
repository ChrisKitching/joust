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

    /**
     * Given a method call, return the VarSymbol, if any, of the object on which the call is being done.
     * Returns null if the callee is not a VarSymbol.
     */
    public static VarSymbol getCalledObjectForCall(AJCCall call) {
        if (!(call.meth instanceof AJCFieldAccess)) {
            return null;
        }
        AJCFieldAccess<MethodSymbol> cast = (AJCFieldAccess<MethodSymbol>) call.meth;

        // Make sure we're not selecting on a literal or something stupid...
        if (!(cast.selected instanceof AJCSymbolRef)) {
            return null;
        }

        AJCSymbolRef selected = (AJCSymbolRef) cast.selected;
        Symbol sym = selected.getTargetSymbol();

        if (sym instanceof VarSymbol) {
            return (VarSymbol) sym;
        }

        return null;
    }

    public static AJCExpressionTree removeCast(AJCTypeCast tree) {
        return removeCast(tree.expr);
    }
    public static AJCExpressionTree removeCast(AJCExpressionTree tree) {
        if (tree instanceof AJCTypeCast) {
            return removeCast(((AJCTypeCast) tree).expr);
        }

        return tree;
    }
}
