package joust.optimisers.avail.normalisedexpressions;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

public class PotentiallyAvailableUnary extends PotentiallyAvailableExpression {
    public PotentiallyAvailableExpression operand;
    private Tag opcode;

    /**
     * Create a PotentiallyAvailableUnary from the given JCUnary.
     */
    public PotentiallyAvailableUnary(PotentiallyAvailableExpression target, Tag op) {
        super();
        operand = target;
        opcode = op;

        deps.add(sym);
        deps.addAll(target.deps);
    }
}
