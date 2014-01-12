package joust.optimisers.avail.normalisedexpressions;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import lombok.AllArgsConstructor;

import java.util.HashSet;

import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

/**
 * A representation of a JCBinary allowing for the use of hypothetical symbols.
 */
public class PotentiallyAvailableBinary extends PotentiallyAvailableExpression {
    public PotentiallyAvailableExpression lhs;
    public PotentiallyAvailableExpression rhs;
    public Tag opcode;

    public PotentiallyAvailableBinary(PotentiallyAvailableExpression l, PotentiallyAvailableExpression r, Tag op) {
        super();
        lhs = l;
        rhs = r;
        opcode = op;

        deps.add(sym);
        // This expression depends on all the dependencies of the subexpressions.
        deps.addAll(l.deps);
        deps.addAll(r.deps);
    }
}
