package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.*;

//TODO: Really? Do we need this any more?
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class PotentiallyAvailableFunctionalExpression extends PotentiallyAvailableExpression {
    public List<PotentiallyAvailableExpression> args = List.nil();
    public MethodSymbol callTarget;

    public PotentiallyAvailableFunctionalExpression(AJCCall e) {
        callTarget = e.getTargetSymbol();
        deps.add(virtualSym);
        sourceNode = e;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PotentiallyAvailableFunctionalExpression)) {
            return false;
        }

        PotentiallyAvailableFunctionalExpression cast = (PotentiallyAvailableFunctionalExpression) obj;

        // Check if this is a call to the same method...
        if (!callTarget.equals(cast.callTarget)) {
            return false;
        }

        // ... And that it has equivalent arguments...
        for (int i = 0; i < args.length(); i++) {
            PotentiallyAvailableExpression pae1 = args.get(i);
            PotentiallyAvailableExpression pae2 = cast.args.get(i);

            if (!pae1.equals(pae2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<AJCStatement> concretify(Symbol owningContext) {
        log.debug("Concretifying {}", this);

        // Copy the associated node.
        AJCCall thisExpr = (AJCCall) treeCopier.copy(sourceNode);

        log.debug("thisExpr:{}", thisExpr);

        // Create a new temporary variable to hold this expression.
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, sourceNode.getNodeType(), owningContext);
        concreteSym = PossibleSymbol.getConcrete(newSym);
        expressionNode = treeMaker.Ident(newSym);

        // TODO
        AJCVariableDecl newDecl = null;//treeMaker.VarDef(newSym, thisExpr);
        log.debug("newDecl:{}", newDecl);

        return List.of((AJCStatement) newDecl);
    }
}
