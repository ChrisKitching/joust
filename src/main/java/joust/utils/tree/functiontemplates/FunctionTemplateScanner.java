package joust.utils.tree.functiontemplates;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class FunctionTemplateScanner extends AJCTreeVisitor {
    public final List[] substitutionPoints;
    private final Type[] paramTypes;

    /**
     * Construct a new template scanner equipped to process a template for a function of a given number of arguments.
     */
    public FunctionTemplateScanner(Type... pTypes) {
        paramTypes = pTypes;
        substitutionPoints = new List[pTypes.length];

        for (int i = 0; i < substitutionPoints.length; i++) {
            substitutionPoints[i] = List.nil();
        }
    }

    @Override
    protected void visitIdent(AJCIdent that) {
        super.visitIdent(that);

        String nameStr = that.getName().toString();
        if (!nameStr.endsWith("$PARAM")) {
            return;
        }

        int paramNum = Integer.parseInt(nameStr.substring(0, nameStr.indexOf('$')));
        if (paramNum < 0 || paramNum >= substitutionPoints.length) {
            log.fatal("Invalid template! Arg index out of bounds: {}", that);
        }

        that.setType(paramTypes[paramNum]);
        substitutionPoints[paramNum] = substitutionPoints[paramNum].prepend(that);
    }
}
