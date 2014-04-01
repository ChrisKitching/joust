package tests.unittests.utils;

import joust.optimisers.visitors.sideeffects.Effects;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.treeinfo.EffectSet;

import static joust.tree.annotatedtree.AJCTree.*;

/**
 * A tree visitor to strip the results of analysis from all tree nodes, ready for the next test run.
 */
public class VisitorResultPurger extends AJCTreeVisitor {
    @Override
    public void visit(AJCTree that) {
        if (that instanceof AJCEffectAnnotatedTree) {
            AJCEffectAnnotatedTree cast = (AJCEffectAnnotatedTree) that;
            cast.liveVariables = null;
            cast.effects = new Effects(EffectSet.ALL_EFFECTS);
        }

        super.visit(that);
    }
}
