package joust.utils.tree.functiontemplates;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.cse.CommonSubExpressionTranslator;
import joust.optimisers.invar.ExpressionComplexityClassifier;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTree.AJCExpressionTree;
import joust.tree.annotatedtree.AJCTree.AJCStatement;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.data.SetHashMap;
import joust.utils.logging.LogUtils;
import joust.utils.tree.NameFactory;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;

/**
 * A template for function inlining. Represents expression to replace the call with.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class FunctionTemplate {
    private AJCExpressionTree template;
    public final boolean isStatic;

    // The nodes of the template that need substituting for the arguments.
    public List[] substitutionPoints;

    Type[] paramTypes;

    private final int numParams;

    // The complexity an argument to a templated function must have before we extract it to a temporary variable.
    public static final int ARGUMENT_EXTRACTION_THRESHOLD = CommonSubExpressionTranslator.MINIMUM_CSE_SCORE;

    public FunctionTemplate(AJCExpressionTree inputTemplate, boolean stat, Type... pTypes) {
        template = inputTemplate;
        isStatic = stat;

        paramTypes = pTypes;
        numParams = paramTypes.length;
        rebuildSubstitutionPoints();
    }

    private void rebuildSubstitutionPoints() {
        FunctionTemplateScanner scanner = new FunctionTemplateScanner(paramTypes);
        scanner.visitTree(template);

        substitutionPoints = scanner.substitutionPoints;
    }

    /**
     * Sometimes, arguments need to be refactored into new temporary variables.
     * This method returns the list of statements that need inserting before an instantation of the template with the
     * given argument set.
     */
    public FunctionTemplateInstance instantiateWithTemps(Symbol enclosingSymbol, AJCExpressionTree... args) {
        // For each argument, determine if it needs to be pulled to a temporary.

        List<AJCStatement> startupCode = List.nil();

        for (int i = 0; i < args.length; i++) {
            // If the argument is only used once, we don't care.
            List<AJCExpressionTree> refsToArg = substitutionPoints[i];
            if (refsToArg.size() <= 1) {
                continue;
            }

            // If the argument has the wrong sort of side effects, we've got no choice but to extract it.
            EffectSet argEffects = args[i].effects.getEffectSet();

            // If it has IO effects, or both escaping reads or writes, it's not safe to do it repeatedly.
            if ((!argEffects.contains(EffectSet.EffectType.READ_ESCAPING)
              || !argEffects.contains(EffectSet.EffectType.WRITE_ESCAPING))
              && !argEffects.contains(EffectSet.EffectType.IO)) {
                 // Since you don't *have* to extract it, check if this one is expensive enough for it to be worth it.
                 ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
                 classifier.visitTree(args[i]);

                 if (classifier.getScore() < ARGUMENT_EXTRACTION_THRESHOLD) {
                     continue;
                 }
            }

            AJCVariableDecl newTemp = extractArgument(args[i], enclosingSymbol);
            startupCode = startupCode.prepend(newTemp);
            args[i] = treeMaker.Ident(newTemp.getTargetSymbol());
        }

        return new FunctionTemplateInstance(instantiate(args), startupCode.reverse());
    }

    private AJCVariableDecl extractArgument(AJCExpressionTree arg, Symbol owningContext) {
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, arg.getNodeType(), owningContext);

        // Create a new temporary variable to hold this expression.
        AJCVariableDecl newDecl = treeMaker.VarDef(newSym, arg);
        return newDecl;
    }

    /**
     * Create an instance of the template function this object represents, populated with the given parameter
     * expressions. Note that the input parameters aren't copied.
     */
    public AJCExpressionTree instantiate(AJCExpressionTree... args) {
        if (args.length > numParams) {
            log.fatal("Unable to instantiate template {} with args {}. Expected {} arguments but got {}!",
                    template, Arrays.toString(args), numParams, args.length);
            return null;
        }

        // Special-case the identity method.
        if (args.length == 1 && ((AJCTree)substitutionPoints[0].head).mParentNode == null) {
            return args[0];
        }

        log.debug("Template: {}", template);

        AJCExpressionTree templateCopy = treeCopier.copy(template);

        for (int i = 0; i < substitutionPoints.length; i++) {
            List<AJCExpressionTree> argRefs = substitutionPoints[i];
            for (AJCExpressionTree argRef : argRefs) {
                AJCExpressionTree argCopy = args[i];

                argRef.swapFor(argCopy);
                log.debug("Swapped {} for {}", argRef, argCopy);
            }
        }

        AJCExpressionTree ret = template;
        template = templateCopy;
        rebuildSubstitutionPoints();

        log.debug("Yields: {}", ret);
        log.debug("Fixed: {}", template);
        return ret;
    }
}
