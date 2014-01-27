package joust.treeinfo;


import com.sun.tools.javac.code.Symbol;
import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to store information related to a particular tree node.
 */
public @Data
class TreeInfo {
    // The side effects of the corresponding tree node.
    EffectSet mEffectSet;

    HashSet<PotentiallyAvailableExpression> potentiallyAvailable;

    Set<Symbol> liveVariables;
}
