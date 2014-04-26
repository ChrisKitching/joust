package joust.optimisers.shortfunc;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import joust.utils.tree.functiontemplates.FunctionTemplate;
import joust.utils.tree.functiontemplates.FunctionTemplateInstance;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.optimisers.shortfunc.ShortFuncFunctionTemplates.functionTemplates;

/**
 * Replace calls to functions with entries in the static function template table with their inlined equivalents.
 * This provides a handy, if annoyingly manual, route for inlining calls to library functions for which call overhead
 * dominates actual work done, such as Math.min.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ShortFuncTranslator extends BaseTranslator {
    private MethodSymbol enclosingMethod;

    @Override
    protected void visitMethodDef(AJCMethodDecl that) {
        enclosingMethod = that.getTargetSymbol();
        super.visitMethodDef(that);

        if (mHasMadeAChange) {
            AJCForest.getInstance().initialAnalysis();
            log.info("After shortFunc:\n {}", that);
        }
    }

    boolean mayEdit = true;

    @Override
    protected void visitBlock(AJCBlock that) {
        for (AJCTree t : that.stats) {
            mayEdit = true;
            visit(t);
        }
    }

    @Override
    protected void visitCall(AJCCall that) {
        super.visitCall(that);

        if (!mayEdit) {
            return;
        }

        MethodSymbol targetSym = that.getTargetSymbol();
        FunctionTemplate template = functionTemplates.get(targetSym);
        log.info("Got {}", targetSym);
        log.info("Avail: {}", functionTemplates.keySet());

        // No template available.
        if (template == null) {
            return;
        }

        mayEdit = false;
        mHasMadeAChange = true;
        FunctionTemplateInstance instance = template.instantiateWithTemps(enclosingMethod, that.args.toArray(new AJCExpressionTree[that.args.size()]));

        log.info("Inserting {} before call {}", instance.startup, that);
        log.info("{} -> {}", that, instance.body);
        that.getEnclosingBlock().insertBefore(that.getEnclosingStatement(), instance.startup);
        that.swapFor(instance.body);

        log.info("Intermediate: \n{}", that.getEnclosingBlock());
    }
}
