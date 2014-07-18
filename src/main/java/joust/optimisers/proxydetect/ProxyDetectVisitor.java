package joust.optimisers.proxydetect;

import com.sun.tools.javac.code.Symbol;
import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.logging.LogUtils;
import joust.utils.tree.JCTreeStructurePrinter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.MethodSymbol;
import static joust.tree.annotatedtree.AJCTree.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class ProxyDetectVisitor extends BaseTranslator {
    @Override
    protected void visitMethodDef(AJCMethodDecl that) {
        super.visitMethodDef(that);

        // Detect methods with names matching that of a proxy method.
        // If you find one, dig in and see what it's calling, printing the culprit.
        String methodName = that.getTargetSymbol().getSimpleName().toString();

        if (!methodName.startsWith("access$")) {
            return;
        }

        AJCBlock body = that.body;

        for (AJCStatement stat : body.stats) {
            if (stat instanceof AJCReturn) {
                // This is a field getter/setter.
                AJCReturn ret = (AJCReturn) stat;

                Symbol sym = ((AJCSymbolRef) ret.expr).getTargetSymbol();

                if (sym instanceof MethodSymbol) {
                    log.warn("Method: {}.{}", sym.owner, sym);
                } else {
                    log.warn("Field: {}.{}", sym.owner, sym);
                }
            } else if (stat instanceof AJCExpressionStatement) {
                AJCCall exTree = (AJCCall) ((AJCExpressionStatement) stat).expr;

                MethodSymbol mSym = exTree.getTargetSymbol();

                log.warn("Method: {}.{}", mSym.owner, mSym);
            }
        }


    }
}
