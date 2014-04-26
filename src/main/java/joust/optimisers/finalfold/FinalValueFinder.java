package joust.optimisers.finalfold;

import com.sun.tools.javac.code.Flags;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.logging.LogUtils;
import joust.utils.tree.evaluation.EvaluationContext;
import joust.utils.tree.evaluation.Value;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class FinalValueFinder extends AJCTreeVisitor {
    public HashMap<VarSymbol, Value> values = new HashMap<>();

    @Override
    protected void visitClassDef(AJCClassDecl that) {
        for (AJCVariableDecl varDef : that.fields) {
            if (varDef.getInit().isEmptyExpression()) {
                continue;
            }

            VarSymbol vSym = varDef.getTargetSymbol();

            if ((vSym.flags() & Flags.STATIC) != 0
             && (vSym.flags() & Flags.FINAL) != 0) {
                log.info("Static final found: {}", varDef);

                // Try to evaluate the initialiser expression now.
                // If the index is something we can evaluate at compile-time, we're okay.
                EvaluationContext context = new EvaluationContext();
                Value index = context.evaluate(varDef.getInit());

                if (index != Value.UNKNOWN) {
                    log.info("Found to be of value: {}", index.getValue());
                    values.put(vSym, index);
                }
            }
        }
    }
}
