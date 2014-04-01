package joust.optimisers.cse;

import java.util.Comparator;

public class AvailableExpressionComparator implements Comparator<AvailableExpression> {
    @Override
    public int compare(AvailableExpression o1, AvailableExpression o2) {
        if (o1.getComplexityScore() > o2.getComplexityScore()) {
            return -1;
        }

        if (o1.getComplexityScore() == o2.getComplexityScore()) {
            return 0;
        }

        return 1;
    }
}
