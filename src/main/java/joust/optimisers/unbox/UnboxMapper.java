package joust.optimisers.unbox;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.functiontemplates.FunctionTemplate;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import joust.utils.tree.NameFactory;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.code.Type.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;
import static joust.optimisers.unbox.UnboxingFunctionTemplates.functionTemplates;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class UnboxMapper {
    private VarSymbol newSym;
    private final VarSymbol oldSym;

    // The parent symbol of the symbol being replaced.
    private final Symbol enclosingMethod;

    private Type unboxedType;

    public UnboxMapper(VarSymbol sym) {
        oldSym = sym;
        enclosingMethod = sym.owner;

        unboxedType = types.unboxedType(oldSym.type);
        if (unboxedType == noType) {
            log.fatal("Unable to find type for {}!", oldSym);
        }

        // Create a new variable definition for the new symbol.
        Name tempName = NameFactory.getName();
        newSym = new VarSymbol(0, tempName, unboxedType, enclosingMethod);
    }

    public AJCTree replacementTree(AJCTree tree) {
        if (tree instanceof AJCIdent) {
            return replacementIdent((AJCIdent) tree);
        } else if (tree instanceof AJCCall) {
            return replacementCall((AJCCall) tree);
        } else if (tree instanceof AJCAssign) {
            return replacementAssign((AJCAssign) tree);
        } else if (tree instanceof AJCVariableDecl) {
            return replacementVarDef((AJCVariableDecl) tree);
        }

        log.fatal("Unable to find replacement tree for {}:{}", tree, tree.getClass().getCanonicalName());
        return null;
    }

    private AJCTree replacementAssign(AJCAssign tree) {
        tree.setType(newSym.type);
        return tree;
    }

    public AJCVariableDecl replacementVarDef(AJCVariableDecl tree) {
        // Create an appropriately modified init tree.
        // If the init tree is a call to one of the valueOf methods, it can be replaced with the argument.
        // If it's any other expression, you need (oldExpr).intValue()  (Or such).
        AJCExpressionTree init = tree.getInit();

        // Declaration thereof. Translation of the init will occur later.
        AJCVariableDecl ret = treeMaker.VarDef(newSym, getConvertedInit(init, unboxedType));
        ret.setType(unboxedType);

        return ret;
    }

    private AJCExpressionTree getConvertedInit(AJCExpressionTree init, Type unboxedType) {
        if (newSym == null) {
            throw new IllegalStateException("Attempt to convert usages of a boxed symbol before converting the declaration.");
        }

        if (init.isEmptyExpression()) {
            return init;
        }

        log.debug("Tree: {}", init);
        log.debug("Current type: {}", init.getNodeType());
        log.debug("Target type : {}", unboxedType);
        if (types.isSameType(init.getNodeType(), unboxedType)) {
            return init;
        }

        if (init instanceof AJCCall) {
            AJCCall cast = (AJCCall) init;

            MethodSymbol calledMethod = cast.getTargetSymbol();

            // Check for valueOf.
            if ("valueOf".equals(calledMethod.name.toString())) {
                // Check it's the one that takes a single argument of type unboxedType.
                if (calledMethod.params.size() != 1) {
                    return getDefaultConvertedInit(init, unboxedType);
                }

                if (types.isSameType(calledMethod.params.head.type, unboxedType)) {
                    return cast.args.head;
                }
            }

            // TODO: Convert valueOf(String) to Integer.valueOf(String) and so on, instead of valueOf(String).intValue().
        }

        return getDefaultConvertedInit(init, unboxedType);
    }

    /**
     * Return (init).intValue() (Or such, for the appropriate required type.)
     */
    private AJCExpressionTree getDefaultConvertedInit(AJCExpressionTree init, Type unboxedType) {
        log.info("unboxedType: {}", unboxedType);
        MethodSymbol xValue = UnboxingFunctionTemplates.unboxedValueFunctions.get(unboxedType);
        log.debug("xValue: {}", xValue);
        AJCIdent<MethodSymbol> methodIdent = treeMaker.Ident(xValue);
        log.debug("ident: {}", methodIdent);
        AJCCall ret = treeMaker.Call(treeMaker.Select(init, xValue), List.<AJCExpressionTree>nil());
        ret.setType(unboxedType);
        log.debug("ret: {}", ret);
        return ret;
    }

    public AJCIdent replacementIdent(AJCIdent that) {
        // If it's a reference to the old symbol, replace it.
        if (that.getTargetSymbol() == oldSym) {
            log.debug("New sym type: {}", newSym.type);
            return treeMaker.Ident(newSym);
        }

        return that;
    }

    protected AJCExpressionTree replacementCall(AJCCall that) {
        if (newSym == null) {
            throw new IllegalStateException("Attempt to convert usages of a boxed symbol before converting the declaration.");
        }

        MethodSymbol calledMethod = that.getTargetSymbol();
        FunctionTemplate template = functionTemplates.get(calledMethod);

        if (template == null) {
            return null;
        }

        if (template.isStatic) {
            AJCExpressionTree replacement = template.instantiate(
                    that.args.toArray(
                            new AJCExpressionTree[that.args.size()]
                    )
            );

            log.info("Replacing {} with {}", that, replacement);
            return replacement;
        } else {
            // For nonstatics, the first argument to the template is the callee.
            List<AJCExpressionTree> newArgs = that.args;
            newArgs = newArgs.prepend(treeMaker.Ident(newSym));

            AJCExpressionTree replacement = template.instantiate(
                    newArgs.toArray(
                            new AJCExpressionTree[newArgs.size()]
                    )
            );

            log.info("Replacing {} with {}", that, replacement);
            return replacement;
        }
    }
}
