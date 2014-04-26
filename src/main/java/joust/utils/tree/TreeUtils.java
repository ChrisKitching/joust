package joust.utils.tree;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import javax.lang.model.type.TypeKind;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.tree.annotatedtree.AJCTree.*;

/**
 * A collection of utility functions for handling JCTree nodes.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class TreeUtils {
    // The scope SENTINEL entry is returned when a lookup fails.
    private static Scope.Entry SENTINEL;

    public static void init() {
        // Extract the SENTINEL value.
        try {
            Class<Scope> scopeClass = Scope.class;
            Field sentinelField = scopeClass.getDeclaredField("sentinel");
            sentinelField.setAccessible(true);
            SENTINEL = (Scope.Entry) sentinelField.get(null);
        } catch (IllegalAccessException e) {
            log.fatal("Unable to initialise SENTINEL field!", e);
        } catch (NoSuchFieldException e) {
            log.fatal("Unable to initialise SENTINEL field!", e);
        }
    }

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
     * Convert a Kind literal to a literal type, if possible.
     */
    public static Type kindToType(Kind input) {
        switch (input) {
            case INT_LITERAL:
                return symtab.intType;
            case LONG_LITERAL:
                return symtab.longType;
            case FLOAT_LITERAL:
                return symtab.floatType;
            case DOUBLE_LITERAL:
                return symtab.doubleType;
            case BOOLEAN_LITERAL:
                return symtab.booleanType;
            case CHAR_LITERAL:
                return symtab.charType;
            case STRING_LITERAL:
                return symtab.stringType;
            case NULL_LITERAL:
                return symtab.botType;
            default:
                throw new UnsupportedOperationException("Unknown primitive type kind encountered: " + input);
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

    public static MethodSymbol findMethod(String mName, ClassSymbol clazz, Type... paramTypes) {
        return findMethod(mName, clazz, false, paramTypes);
    }
    public static MethodSymbol findMethod(String mName, ClassSymbol clazz, boolean findStatic, Type... paramTypes) {
        log.debug("Seeking {} on {}", mName, clazz);

        Scope members = clazz.members();
        final Name methodName = names.fromString(mName);

        // Gets us all the things with the right name, irrespective of type or signature.
        Scope.Entry scopeEntry = members.lookup(methodName);

        if (scopeEntry == SENTINEL) {
            log.fatal("Sentinel node encountered looking for {} on {}", mName, clazz);
            return null;
        }

        MethodSymbol resultSym = null;

        // Flick through the results to find the one that matches the signature we want.
        scopeIteration:
        for (;scopeEntry != SENTINEL; scopeEntry = scopeEntry.next()) {
            // Not a method - skip!
            if (!(scopeEntry.sym instanceof MethodSymbol)) {
                log.debug("Binning: Nonmethod");
                continue;
            }

            MethodSymbol mSym = (MethodSymbol) scopeEntry.sym;

            if (findStatic) {
                // Is this a static function?
                if ((mSym.flags() & Flags.STATIC) == 0) {
                    log.debug("Binning: Nonstatic");
                    continue;
                }
            }

            // Check that the parameters match up.
            List<VarSymbol> realParams = mSym.params();
            if (realParams.length() != paramTypes.length) {
                log.debug("Binning: Expected {} args, found {}.", paramTypes.length, realParams.length());
                continue;
            }

            log.debug("Got {}", mSym);
            int pIndex = 0;
            for (VarSymbol sym : realParams) {
                if (!types.isSameType(sym.type, paramTypes[pIndex])) {
                    log.debug("Binning: Argument {} of {} should be {}", pIndex, sym.type, paramTypes[pIndex]);
                    continue scopeIteration;
                }

                pIndex++;
            }

            resultSym = mSym;
            break;
        }

        if (resultSym == null) {
            log.fatal("Failed to find {} on {}", mName, clazz);
            return null;
        }

        return resultSym;
    }

    /**
     * Get the default initialiser expression for a given field type.
     */
    public static JCExpression getDefaultLiteralValueForType(Type t) {
        // Object types default to null.
        if (!t.isPrimitive()) {
            return javacTreeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
        }

        // Numerical types default to zero, booleans to false.
        Type.JCPrimitiveType cast = (Type.JCPrimitiveType) t;
        switch(cast.getTag()) {
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
                return javacTreeMaker.TypeCast(cast, javacTreeMaker.Literal(0).setType(t));
            case LONG:
                return javacTreeMaker.Literal(0L);
            case FLOAT:
                return javacTreeMaker.Literal(0.0F);
            case DOUBLE:
                return javacTreeMaker.Literal(0.0D);
            case BOOLEAN:
                return javacTreeMaker.Literal(false);
            default:
                return javacTreeMaker.Literal(TypeTag.BOT, null).setType(symtab.botType);
        }
    }
}
