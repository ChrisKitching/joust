package joust.optimisers.unbox;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.utils.logging.LogUtils;
import joust.utils.tree.functiontemplates.FunctionTemplate;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.utils.tree.TreeUtils.*;

/**
 * Static utility class holding the hard-coded function templates for unboxing.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public final class UnboxingFunctionTemplates {
    public static Map<MethodSymbol, FunctionTemplate> functionTemplates;

    public static Set<FunctionTemplate> functionTemplatesNeedingArgCheck;

    // Maps unboxed types to the corresponding BoxedClass.*Value() methods.
    public static Map<Type, MethodSymbol> unboxedValueFunctions;

    /**
     * Create the function templates that will be necessary to eliminate calls to methods of boxing classes.
     */
    public static void init() {
        unboxedValueFunctions = new HashMap<>();
        functionTemplatesNeedingArgCheck = new HashSet<>();
        functionTemplates = new HashMap<>();

        // In each case, the 0th param is taken to be the unboxed object.

        // Only exists on Boolean
        AJCExpressionTree booleanValue = param(0, symtab.booleanType);

        AJCExpressionTree shortHashCode = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.INT),
                param(0, symtab.shortType));

        AJCExpressionTree byteHashCode = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.INT),
                param(0, symtab.byteType));

        AJCExpressionTree intHashCode = param(0, symtab.intType);

        AJCExpressionTree longHashCode =
                treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.INT),
                        treeMaker.Binary(JCTree.Tag.BITXOR,
                                param(0, symtab.longType),
                                treeMaker.Binary(JCTree.Tag.USR,
                                        param(0, symtab.longType),
                                        treeMaker.Literal(32))));

        AJCExpressionTree boolHashCode =
                treeMaker.Conditional(param(0, symtab.booleanType),
                        treeMaker.Literal(1231),
                        treeMaker.Literal(1237));

        // Float/Double hashcodes are not supported. They're complicated.

        AJCExpressionTree boolToString =
                treeMaker.Conditional(param(0, symtab.booleanType),
                        treeMaker.Literal("true"),
                        treeMaker.Literal("false"));
        // BoxedClass.toString(someInt), instead of boxedInstance.toString().

        // Go look up the MethodSymbols for the things we mean to replace. This is unpleasant to do.
        ClassSymbol ByteClass = types.boxedClass(symtab.byteType);
        ClassSymbol ShortClass = types.boxedClass(symtab.shortType);
        ClassSymbol IntegerClass = types.boxedClass(symtab.intType);
        ClassSymbol LongClass = types.boxedClass(symtab.longType);
        ClassSymbol BooleanClass = types.boxedClass(symtab.booleanType);
        ClassSymbol FloatClass = types.boxedClass(symtab.floatType);
        ClassSymbol DoubleClass = types.boxedClass(symtab.doubleType);

        // Populate the SENTINEL node so lookups can actually occur...

        Scope intMembers = IntegerClass.members();

        // Byte
        putCommonMethods(symtab.byteType, ByteClass, true);
        putToStringShortByte(symtab.byteType, ByteClass, IntegerClass);
        functionTemplates.put(findMethod("hashCode", ByteClass), new FunctionTemplate(byteHashCode, false, symtab.byteType));

        unboxedValueFunctions.put(symtab.byteType, findMethod("byteValue", ByteClass));

        // Short
        putCommonMethods(symtab.shortType, ShortClass, true);
        putToStringShortByte(symtab.shortType, ShortClass, IntegerClass);
        functionTemplates.put(findMethod("hashCode", ShortClass), new FunctionTemplate(shortHashCode, false, symtab.shortType));

        unboxedValueFunctions.put(symtab.shortType, findMethod("shortValue", ShortClass));

        // Integer
        putCommonMethods(symtab.intType, IntegerClass, true);
        putToString(symtab.intType, IntegerClass);
        functionTemplates.put(findMethod("hashCode", IntegerClass), new FunctionTemplate(intHashCode, false, symtab.intType));

        unboxedValueFunctions.put(symtab.intType, findMethod("intValue", IntegerClass));

        // Long
        putCommonMethods(symtab.longType, LongClass, true);
        putToString(symtab.longType, LongClass);
        functionTemplates.put(findMethod("hashCode", LongClass), new FunctionTemplate(longHashCode, false, symtab.longType));

        unboxedValueFunctions.put(symtab.longType, findMethod("longValue", LongClass));

        // Float
        putCommonMethods(symtab.floatType, FloatClass, false);
        putToString(symtab.floatType, FloatClass);

        unboxedValueFunctions.put(symtab.floatType, findMethod("floatValue", FloatClass));

        // Double
        putCommonMethods(symtab.doubleType, DoubleClass, false);
        putToString(symtab.doubleType, DoubleClass);

        unboxedValueFunctions.put(symtab.doubleType, findMethod("doubleValue", DoubleClass));

        // Boolean
        functionTemplates.put(findMethod("hashCode", BooleanClass), new FunctionTemplate(boolHashCode,false,  symtab.booleanType));
        functionTemplates.put(findMethod("toString", BooleanClass), new FunctionTemplate(boolToString, false, symtab.booleanType));
        functionTemplates.put(findMethod("booleanValue", BooleanClass), new FunctionTemplate(booleanValue, false, symtab.booleanType));

        log.info("Available: {}", Arrays.toString(functionTemplates.keySet().toArray()));
    }

    @SuppressWarnings("unchecked")
    private static void putToString(Type t, ClassSymbol clazz) {
        MethodSymbol toStringSym = findMethod("toString", clazz);

        // Find the static toString method.
        AJCFieldAccess<MethodSymbol> methodRef = treeMaker.Select(treeMaker.Ident(clazz), toStringSym);

        AJCCall toString = treeMaker.Call(methodRef, List.<AJCExpressionTree>of(param(0, t)));

        functionTemplates.put(toStringSym, new FunctionTemplate(toString, false, t));
    }

    /**
     * Byte and Short have toString functions that delegate to Integer. Might as well do that directly.
     */
    @SuppressWarnings("unchecked")
    private static void putToStringShortByte(Type t, ClassSymbol clazz, ClassSymbol intClazz) {
        AJCTypeCast cast = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.INT), param(0, t));

        MethodSymbol intToStringSym = findMethod("toString", intClazz, true, symtab.intType);
        AJCFieldAccess<MethodSymbol> methodRef = treeMaker.Select(treeMaker.Ident(intClazz), intToStringSym);

        AJCCall toString = treeMaker.Call(methodRef, List.<AJCExpressionTree>of(cast));

        functionTemplates.put(findMethod("toString", clazz), new FunctionTemplate(toString, false, t));
    }

    private static void putCommonMethods(Type t, ClassSymbol clazz, boolean compEq) {
        AJCExpressionTree byteValue;
        AJCExpressionTree shortValue;
        AJCExpressionTree intValue;
        AJCExpressionTree longValue;
        AJCExpressionTree floatValue;
        AJCExpressionTree doubleValue;

        if (t.getTag() == TypeTag.BYTE) {
            byteValue = param(0, t);
        } else {
            byteValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.BYTE), param(0, t));
        }

        if (t.getTag() == TypeTag.SHORT) {
            shortValue = param(0, t);
        } else {
            shortValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.SHORT), param(0, t));
        }

        if (t.getTag() == TypeTag.INT) {
            intValue = param(0, t);
        } else {
            intValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.INT), param(0, t));
        }

        if (t.getTag() == TypeTag.LONG) {
            longValue = param(0, t);
        } else {
            longValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.LONG), param(0, t));
        }

        if (t.getTag() == TypeTag.FLOAT) {
            floatValue = param(0, t);
        } else {
            floatValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.FLOAT), param(0, t));
        }

        if (t.getTag() == TypeTag.DOUBLE) {
            doubleValue = param(0, t);
        } else {
            doubleValue = treeMaker.TypeCast(treeMaker.TypeIdent(TypeTag.DOUBLE), param(0, t));
        }

        AJCIdent valueOf = param(0, t);

        if (compEq) {
            AJCExpressionTree compare = treeMaker.Binary(JCTree.Tag.MINUS, param(0, t), param(1, t));

            // Can only be applied if the target object is also being unboxed.
            AJCExpressionTree equals = treeMaker.Binary(JCTree.Tag.EQ, param(0, t), param(1, t));

            FunctionTemplate equalsTemplate = new FunctionTemplate(equals, false, t, t);
            FunctionTemplate compareTemplate = new FunctionTemplate(compare, false, t, t);

            functionTemplates.put(findMethod("equals", clazz, symtab.objectType), equalsTemplate);
            functionTemplates.put(findMethod("compareTo", clazz, clazz.type), compareTemplate);

            functionTemplatesNeedingArgCheck.add(equalsTemplate);
            functionTemplatesNeedingArgCheck.add(compareTemplate);
        }

        functionTemplates.put(findMethod("byteValue", clazz), new FunctionTemplate(byteValue, false, t));
        functionTemplates.put(findMethod("shortValue", clazz), new FunctionTemplate(shortValue, false, t));
        functionTemplates.put(findMethod("intValue", clazz), new FunctionTemplate(intValue, false, t));
        functionTemplates.put(findMethod("longValue", clazz), new FunctionTemplate(longValue, false, t));
        functionTemplates.put(findMethod("floatValue", clazz), new FunctionTemplate(floatValue, false, t));
        functionTemplates.put(findMethod("doubleValue", clazz), new FunctionTemplate(doubleValue, false, t));
        functionTemplates.put(findMethod("valueOf", clazz, true, t), new FunctionTemplate(valueOf, true, t));
    }

    private static AJCIdent param(int index, Type type) {
        AJCIdent ident = treeMaker.Ident(names.fromString(index + "$PARAM"));
        ident.setType(type);

        return ident;
    }

}
