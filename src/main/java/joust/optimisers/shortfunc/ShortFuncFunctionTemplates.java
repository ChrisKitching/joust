package joust.optimisers.shortfunc;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import joust.utils.tree.functiontemplates.FunctionTemplate;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.tree.JCTree.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.utils.tree.TreeUtils.findMethod;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ShortFuncFunctionTemplates {
    public static Map<MethodSymbol, FunctionTemplate> functionTemplates;

    public static void init() {
        functionTemplates = new HashMap<MethodSymbol, FunctionTemplate>();

        ClassSymbol langMath = reader.enterClass(names.fromString("java.lang.Math"));
        // Math.min
        functionTemplates.put(findMethod("min", langMath, true, symtab.intType, symtab.intType), getMinMax(symtab.intType, true));
        functionTemplates.put(findMethod("min", langMath, true, symtab.longType, symtab.longType), getMinMax(symtab.longType, true));

        // Math.max
        functionTemplates.put(findMethod("max", langMath, true, symtab.intType, symtab.intType), getMinMax(symtab.intType, false));
        functionTemplates.put(findMethod("max", langMath, true, symtab.longType, symtab.longType), getMinMax(symtab.longType, false));
        // Not supporting floating point types here - it's not as trivial.

        // Math.abs
        functionTemplates.put(findMethod("abs", langMath, true, symtab.intType), getAbs(symtab.intType));
        functionTemplates.put(findMethod("abs", langMath, true, symtab.longType), getAbs(symtab.longType));
        functionTemplates.put(findMethod("abs", langMath, true, symtab.floatType), getAbs(symtab.floatType));
        functionTemplates.put(findMethod("abs", langMath, true, symtab.doubleType), getAbs(symtab.doubleType));

        // Math.toDegrees
        AJCExpressionTree toDegrees = treeMaker.Binary(Tag.MUL, param(0, symtab.doubleType), treeMaker.Literal(57.2957795131D));
        toDegrees.setType(symtab.doubleType);
        functionTemplates.put(findMethod("toDegrees", langMath, true, symtab.doubleType), new FunctionTemplate(toDegrees, true, symtab.doubleType));

        // Math.toRadians
        AJCExpressionTree toRadians = treeMaker.Binary(Tag.MUL, param(0, symtab.doubleType), treeMaker.Literal(0.0174532925D));
        toDegrees.setType(symtab.doubleType);
        functionTemplates.put(findMethod("toRadians", langMath, true, symtab.doubleType), new FunctionTemplate(toRadians, true, symtab.doubleType));

        ClassSymbol langString = reader.enterClass(names.fromString("java.lang.String"));
        // String.valueOf
        AJCExpressionTree valueOfBoolean = treeMaker.Conditional(param(0, symtab.booleanType), treeMaker.Literal("true"), treeMaker.Literal("false"));
        functionTemplates.put(findMethod("valueOf", langString, true, symtab.booleanType), new FunctionTemplate(valueOfBoolean, true, symtab.booleanType));

        functionTemplates.put(findMethod("valueOf", langString, true, symtab.intType), stringValueOf(symtab.intType));
        functionTemplates.put(findMethod("valueOf", langString, true, symtab.longType), stringValueOf(symtab.longType));
        functionTemplates.put(findMethod("valueOf", langString, true, symtab.floatType), stringValueOf(symtab.floatType));
        functionTemplates.put(findMethod("valueOf", langString, true, symtab.doubleType), stringValueOf(symtab.doubleType));

        // String.toString()...
        functionTemplates.put(findMethod("toString", langString, false), new FunctionTemplate(param(0, symtab.stringType), false, symtab.stringType));
    }

    private static FunctionTemplate stringValueOf(Type t) {
        ClassSymbol boxingClass = types.boxedClass(t);

        // Find the static toString function on the boxing class.
        AJCFieldAccess<MethodSymbol> methodRef = treeMaker.Select(treeMaker.Ident(boxingClass), findMethod("toString", boxingClass, true, t));

        // Make a call to it with an argument of the appropriate type.
        AJCCall toString = treeMaker.Call(methodRef, List.<AJCExpressionTree>of(param(0, t)));

        return new FunctionTemplate(toString, true, t);
    }

    /**
     * Get a FunctionTemplate for one of the Math.min/Math.max methods.
     * @param t The argument type of the method to get.
     * @param min If true, get a Math.min method, else a Math.max one.
     */
    private static FunctionTemplate getMinMax(Type t, boolean min) {
        AJCExpressionTree mathMin =
            treeMaker.Conditional(
                treeMaker.Binary(min ? Tag.LE : Tag.GE, param(0, t), param(1, t)),
                param(0, t),
                param(1, t)
            );
        mathMin.setType(t);

        return new FunctionTemplate(mathMin, true, t, t);
    }

    /**
     * Get a FunctionTemplate representing Math.abs on the given type.
     */
    private static FunctionTemplate getAbs(Type t) {
        // Determine if the input is a floating point type.
        TypeTag tag = t.getTag();
        boolean isFP = tag == TypeTag.FLOAT ||
                       tag == TypeTag.DOUBLE;

        AJCExpressionTree abs =
            treeMaker.Conditional(
                    treeMaker.Binary(Tag.LE, param(0, t), getZero(t)),
                    isFP ?
                        treeMaker.Binary(Tag.MINUS, getZero(t), param(0, t))
                    :
                        treeMaker.Unary(Tag.NEG, param(0, t)),
                    param(0, t)
            );
        abs.setType(t);

        return new FunctionTemplate(abs, true, t);
    }

    /**
     * Get an appropriately-typed zero literal for a given primitive type.
     */
    private static AJCLiteral getZero(Type t) {
        switch(t.getTag()) {
            case DOUBLE:
                return treeMaker.Literal(0.0D);
            case FLOAT:
                return treeMaker.Literal(0.0F);
            case INT:
                return treeMaker.Literal(0);
            case LONG:
                return treeMaker.Literal(0L);
            default:
                log.fatal("Getting zero literal for unknown type: {}", t);
        }

        return null;
    }

    private static AJCIdent param(int index, Type type) {
        AJCIdent ident = treeMaker.Ident(names.fromString(index + "$PARAM"));
        ident.setType(type);

        return ident;
    }
}
