package joust.optimisers.shortfunc;

import com.sun.tools.javac.util.List;
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

        // No template available.
        if (template == null) {
            return;
        }

        mayEdit = false;
        mHasMadeAChange = true;
        FunctionTemplateInstance instance;
        if (template.isStatic) {
            instance = template.instantiateWithTemps(enclosingMethod, that.args.toArray(new AJCExpressionTree[that.args.size()]));
        } else {
            AJCExpressionTree[] args = new AJCExpressionTree[that.args.size()+1];
            if (that.meth instanceof AJCFieldAccess) {
                // TODO: Urgh.
                args[0] = (AJCExpressionTree) ((AJCFieldAccess) that.meth).selected;
            }

            int i = 1;
            for (AJCExpressionTree arg : that.args) {
                args[i] = arg;
                i++;
            }

            instance = template.instantiateWithTemps(enclosingMethod, args);
        }

        List<AJCStatement> startupCopy = List.nil();
        if (!instance.startup.isEmpty()) {
            for (AJCStatement st : instance.startup) {
                startupCopy = startupCopy.prepend(st);
            }

            AJCStatement enclosingStatement = that.getEnclosingStatement();
            if (!(enclosingStatement.mParentNode instanceof AJCCase)) {
                that.getEnclosingBlock().insertBefore(enclosingStatement, instance.startup);
            } else {
                ((AJCCase) enclosingStatement.mParentNode).insertBefore(enclosingStatement, instance.startup);
            }
        }

        log.info("{} -> {}", that, instance.body);
        that.swapFor(instance.body);

        log.info("After shortfunc: \n{}", that.getEnclosingBlock());

        for (AJCEffectAnnotatedTree t : startupCopy) {
            log.info("Repeat: {}", t);
            if (!AJCForest.getInstance().repeatAnalysis(t)) {
                return;
            }
        }

        AJCForest.getInstance().repeatAnalysis(instance.body);
        log.info("Done.");
    }
}
