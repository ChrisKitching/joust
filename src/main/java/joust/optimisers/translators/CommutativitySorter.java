package joust.optimisers.translators;

import joust.optimisers.utils.CommutativitySorterComparator;
import joust.tree.annotatedtree.AJCTreeVisitorImpl;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static com.sun.tools.javac.tree.JCTree.Tag;
import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Arranges commutative subtrees into a standard structure, so semantically equivalent expressions that
 * differ in structure via commutativity are all transformed into identical trees. (Although we'll still
 * miss equivalent trees that are differently-structured via a distribution relationship, alas.).
 */
@Log4j2
public class CommutativitySorter extends AJCTreeVisitorImpl {
    // The commutative operator which combines elements in the commutative subtree of interest.
    private final Tag currentSubtreeTag;

    private final AJCBinary targetTree;

    // The list of elements combined by the commutative subtree of interest.
    private final ArrayList<AJCExpression> combinedElements = new ArrayList<>();

    public CommutativitySorter(AJCBinary tree) {
        currentSubtreeTag = tree.getTag();
        targetTree = tree;
    }

    public AJCBinary process() {
        // Scan the tree and populate combinedElements.
        visit(targetTree);

        if (combinedElements.size() == 1) {
            return targetTree;
        }

        // Sort the elements into a standard order.
        combinedElements.sort(new CommutativitySorterComparator());

        log.info("Elements: {}", Arrays.toString(combinedElements.toArray()));

        // Build the tree from the sorted elements.
        Iterator<AJCExpression> iterator = combinedElements.iterator();
        AJCBinary replacementTree = treeMaker.Binary(currentSubtreeTag, iterator.next(), iterator.next());
        replacementTree.setType(targetTree.getType());
        replacementTree.setOperator(targetTree.getOperator());

        while (iterator.hasNext()) {
            AJCExpression expr = iterator.next();
            replacementTree = treeMaker.Binary(currentSubtreeTag, replacementTree, expr);
            replacementTree.setType(targetTree.getType());
            replacementTree.setOperator(targetTree.getOperator());
        }

        // Return result in some magical way...
        return replacementTree;
    }

    @Override
    public void visitBinary(AJCBinary tree) {
        if (tree.getTag() != currentSubtreeTag) {
            // If it's not a continuation of the commutative subtree, it's a leaf thereof...
            combinedElements.add(tree);
            return;
        }

        // Even if it is, check if this is secretly string concatenation...
        if (currentSubtreeTag == Tag.PLUS) {
            log.info("Node: {}", tree.lhs);
            log.info("Node type: {}", tree.lhs.getType());
            log.info("Node type: {}", tree.rhs.getType());
            if ("java.lang.String".equals(tree.lhs.getType().toString())
             || "java.lang.String".equals(tree.rhs.getType().toString())) {
                log.info("String concat!");
                combinedElements.add(tree);
                return;
            }
        }

        super.visitBinary(tree);
    }

    @Override
    public void visitUnary(AJCUnary tree) {
        combinedElements.add(tree);
    }

    @Override
    public void visitUnaryAsg(AJCUnaryAsg tree) {
        combinedElements.add(tree);
    }

    @Override
    public void visitCall(AJCCall jcMethodInvocation) {
        combinedElements.add(jcMethodInvocation);
    }

    @Override
    public void visitIdent(AJCIdent jcIdent) {
        combinedElements.add(jcIdent);
    }

    @Override
    public void visitFieldAccess(AJCFieldAccess jcFieldAccess) {
        combinedElements.add(jcFieldAccess);
    }

    @Override
    public void visitInstanceOf(AJCInstanceOf jcInstanceOf) {
        combinedElements.add(jcInstanceOf);
    }

    @Override
    public void visitLiteral(AJCLiteral jcLiteral) {
        combinedElements.add(jcLiteral);
    }

    @Override
    public void visitTypeCast(AJCTypeCast jcTypeCast) {
        combinedElements.add(jcTypeCast);
    }

    @Override
    public void visitAssignop(AJCAssignOp jcAssignOp) {
        combinedElements.add(jcAssignOp);
    }

    @Override
    public void visitAssign(AJCAssign jcAssign) {
        combinedElements.add(jcAssign);
    }

    @Override
    public void visitConditional(AJCConditional jcConditional) {
        combinedElements.add(jcConditional);
    }
}
