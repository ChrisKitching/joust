package joust.optimisers.invar;

import joust.tree.annotatedtree.AJCTreeVisitor;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static com.sun.tools.javac.tree.JCTree.Tag.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag;

/**
 * A visitor to scan an expression tree and compute a score representing the cost of
 * evaluating that expression. Used to determine when it is beneficial to perform
 * certain transformations.
 */
public class ExpressionComplexityClassifier extends AJCTreeVisitor {
    // The additional price to associate with an operation if it involves an assignment.
    private static final int ASSIGNMENT_COST = 2;

    private static final Map<Tag, Integer> operationCosts;
    static {
        Map<Tag, Integer> map = new EnumMap<Tag, Integer>(Tag.class) {
            {
                // TODO: Benchmark and tune these parameters...
                // Sign changes and bitwise operations considered cheapest...
                put(POS, 1);
                put(NEG, 1);
                put(BITXOR, 2);
                put(BITAND, 2);
                put(BITOR, 2);
                put(COMPL, 2);
                put(SL, 2);
                put(SR, 2);
                put(USR, 2);

                // Addition less cheap...
                put(MINUS, 3);
                put(PLUS, 3);
                put(MOD, 3);

                // Logical operators
                put(GE, 2);
                put(LE, 2);
                put(LT, 2);
                put(GT, 2);
                put(EQ, 2);
                put(NE, 2);
                put(NOT, 1);
                put(AND, 2);
                put(OR, 2);

                // Division costly.
                put(MUL, 3);
                put(DIV, 4);

                // Op-asg variants...
                put(BITXOR_ASG, get(BITXOR) + ASSIGNMENT_COST);
                put(BITOR_ASG, get(BITOR) + ASSIGNMENT_COST);
                put(BITAND_ASG, get(BITAND) + ASSIGNMENT_COST);
                put(SL_ASG, get(SL) + ASSIGNMENT_COST);
                put(SR_ASG, get(SR) + ASSIGNMENT_COST);
                put(USR_ASG, get(USR) + ASSIGNMENT_COST);
                put(MINUS_ASG, get(MINUS) + ASSIGNMENT_COST);
                put(PLUS_ASG, get(PLUS) + ASSIGNMENT_COST);
                put(MUL_ASG, get(MUL) + ASSIGNMENT_COST);
                put(DIV_ASG, get(DIV) + ASSIGNMENT_COST);
                put(MOD_ASG, get(MOD) + ASSIGNMENT_COST);

                // instanceof
                put(TYPETEST, 2);
                put(SELECT, 2);
            }
        };
        operationCosts = Collections.unmodifiableMap(map);
    }

    @Getter
    private int score;

    @Override
    protected void visitFieldAccess(AJCFieldAccess that) {
        score += operationCosts.get(that.getTag());
        super.visitFieldAccess(that);
    }

    @Override
    protected void visitBinary(AJCBinary that) {
        score += operationCosts.get(that.getTag());
        super.visitBinary(that);
    }

    @Override
    protected void visitUnary(AJCUnary that) {
        score += operationCosts.get(that.getTag());
        super.visitUnary(that);
    }

    @Override
    protected void visitUnaryAsg(AJCUnaryAsg that) {
        score += operationCosts.get(that.getTag());
        super.visitUnaryAsg(that);
    }

    @Override
    protected void visitTypeCast(AJCTypeCast that) {
        score += operationCosts.get(that.getTag());
        super.visitTypeCast(that);
    }

    @Override
    protected void visitInstanceOf(AJCInstanceOf that) {
        score += operationCosts.get(that.getTag());
        super.visitInstanceOf(that);
    }
}
