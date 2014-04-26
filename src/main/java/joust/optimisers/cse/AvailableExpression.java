package joust.optimisers.cse;

import joust.optimisers.invar.ExpressionComplexityClassifier;
import joust.tree.annotatedtree.AJCComparableExpressionTree;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;

public class AvailableExpression {
    // The first encountered instance of this expression. Useful for introducing the temporary variable.
    // Note that multiple AvailableExpression objects with equivalent but different firstInstance expressions may exist
    // if intervening code invalidates earlier ones.
    public final AJCComparableExpressionTree<? extends AJCExpressionTree> firstInstance;

    // The sum of the complexity scores of each instance of this expression.
    // If this value is greater than the cost of the temporary plus |usagesInternal| * TEMP_VAR_READ_COST the transformation
    // is performed.
    @Getter private int complexityScore;

    public AvailableExpression(AJCComparableExpressionTree<? extends AJCExpressionTree> first) {
        firstInstance = first;
        addUsage(first);
    }

    // Each use of this expression that could potentially be replaced with a temporary if we apply CSE.
    private final List<AJCComparableExpressionTree> usagesInternal = new ArrayList<AJCComparableExpressionTree>();
    public final List<AJCComparableExpressionTree> usages = Collections.unmodifiableList(usagesInternal);

    public void addUsage(AJCComparableExpressionTree tree) {
        usagesInternal.add(tree);

        ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
        classifier.visitTree(tree.wrappedNode);
        complexityScore += classifier.getScore();
    }

    /**
     * Get the cost of the expressions of interest if CSE were applied here.
     */
    public int getApplicationCost() {
        ExpressionComplexityClassifier classifier = new ExpressionComplexityClassifier();
        classifier.visitTree(firstInstance.wrappedNode);

        // The cost of introducing the new temporary.
        int initialiserCost = classifier.getScore() + ExpressionComplexityClassifier.ASSIGNMENT_COST;

        int usagesCost = usagesInternal.size() * ExpressionComplexityClassifier.IDENT_COST;

        return initialiserCost + usagesCost;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AvailableExpression(" + complexityScore + ", " + usagesInternal.size() + "): ");
        sb.append(firstInstance.wrappedNode).append("\nUsages:\n");
        for (AJCComparableExpressionTree t : usagesInternal) {
            sb.append(t.wrappedNode).append('\n');
        }

        return sb.toString();
    }
}
