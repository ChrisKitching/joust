package joust.treeinfo;


import joust.optimisers.avail.normalisedexpressions.PotentiallyAvailableExpression;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Used to store information related to a particular tree node.
 */
public @Data
class TreeInfo {
    // The side effects of the corresponding tree node.
    EffectSet mEffectSet;

    HashSet<PotentiallyAvailableExpression> potentiallyAvailable;
}
