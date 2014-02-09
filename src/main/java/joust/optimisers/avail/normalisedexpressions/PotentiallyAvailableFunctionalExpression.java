package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.optimisers.utils.NonStupidTreeCopier;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;
import static joust.Optimiser.treeMaker;

public @Log4j2
class PotentiallyAvailableFunctionalExpression extends PotentiallyAvailableNullary {
    public List<PotentiallyAvailableExpression> args = List.nil();
    public MethodSymbol callTarget;

    public PotentiallyAvailableFunctionalExpression(JCMethodInvocation e) {
        super(e);
        callTarget = TreeUtils.getTargetSymbolForCall(e);
        deps.add(virtualSym);
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
    public List<JCStatement> concretify(Symbol owningContext) {
        log.debug("Concretifying {}", this);

        // Create a node representing this call
        NonStupidTreeCopier<Void> copier = new NonStupidTreeCopier<>(treeMaker);

        // Copy the associated node.
        JCMethodInvocation thisExpr = (JCMethodInvocation) copier.visitMethodInvocation((JCMethodInvocation) expr, null);

        log.debug("thisExpr:{}", thisExpr);

        // Create a new temporary variable to hold this expression.
        Name tempName = NameFactory.getName();
        VarSymbol newSym = new VarSymbol(Flags.FINAL, tempName, sourceNode.type, owningContext);
        concreteSym = PossibleSymbol.getConcrete(newSym);
        expressionNode = treeMaker.Ident(newSym);

        JCVariableDecl newDecl = treeMaker.VarDef(newSym, thisExpr);
        log.debug("newDecl:{}", newDecl);

        return List.of((JCStatement) newDecl);
    }
}
