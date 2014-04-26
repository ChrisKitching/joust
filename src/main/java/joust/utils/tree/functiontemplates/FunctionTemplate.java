package joust.utils.tree.functiontemplates;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTree.AJCExpressionTree;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static joust.utils.compiler.StaticCompilerUtils.*;

/**
 * A template for function inlining. Represents expression to replace the call with.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class FunctionTemplate {
    private final AJCExpressionTree template;
    public final boolean isStatic;

    // The nodes of the template that need substituting for the arguments.
    public final List[] substitutionPoints;

    private final int numParams;

    public FunctionTemplate(AJCExpressionTree inputTemplate, boolean stat, Type... paramTypes) {
        template = inputTemplate;
        isStatic = stat;

        numParams = paramTypes.length;

        FunctionTemplateScanner scanner = new FunctionTemplateScanner(paramTypes);
        scanner.visitTree(inputTemplate);

        substitutionPoints = scanner.substitutionPoints;
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

        Map<AJCTree, AJCTree> backwardSubs = new HashMap<>();

        for (int i = 0; i < substitutionPoints.length; i++) {
            List<AJCExpressionTree> argRefs = substitutionPoints[i];
            for (AJCExpressionTree argRef : argRefs) {
                AJCExpressionTree argCopy = args[i];
                backwardSubs.put(argCopy, argRef);

                argRef.swapFor(argCopy);
            }
        }

        AJCExpressionTree templateCopy = treeCopier.copy(template);

        // Undo the damage we just did to the template...
        for (AJCTree sub : backwardSubs.keySet()) {
            sub.swapFor(backwardSubs.get(sub));
        }

        return templateCopy;
    }
}
