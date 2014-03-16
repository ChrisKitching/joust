package joust.optimisers.translators;

import joust.optimisers.utils.CommutativitySorterComparator;
import joust.optimisers.visitors.DepthFirstJavacTreeVisitor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static com.sun.tools.javac.tree.JCTree.*;
import static joust.Optimiser.treeMaker;

/**
 * Arranges commutative subtrees into a standard structure, so semantically equivalent expressions that
 * differ in structure via commutativity are all transformed into identical trees. (Although we'll still
 * miss equivalent trees that are differently-structured via a distribution relationship, alas.).
 */
public @Log4j2 class CommutativitySorter extends DepthFirstJavacTreeVisitor {
    // The commutative operator which combines elements in the commutative subtree of interest.
    private Tag currentSubtreeTag;

    private JCBinary targetTree;

    // The list of elements combined by the commutative subtree of interest.
    private ArrayList<JCExpression> combinedElements = new ArrayList<>();

    public CommutativitySorter(JCBinary tree) {
        currentSubtreeTag = tree.getTag();
        targetTree = tree;
    }

    public JCBinary process() {
        // Scan the tree and populate combinedElements.
        targetTree.accept(this);

        if (combinedElements.size() == 1) {
            return targetTree;
        }

        // Sort the elements into a standard order.
        combinedElements.sort(new CommutativitySorterComparator());

        log.info("Elements: {}", Arrays.toString(combinedElements.toArray()));

        // Build the tree from the sorted elements.
        Iterator<JCExpression> iterator = combinedElements.iterator();
        JCBinary replacementTree = treeMaker.Binary(currentSubtreeTag, iterator.next(), iterator.next());
        replacementTree.type = targetTree.type;
        replacementTree.operator = targetTree.operator;

        while (iterator.hasNext()) {
            JCExpression expr = iterator.next();
            replacementTree = treeMaker.Binary(currentSubtreeTag, replacementTree, expr);
            replacementTree.type = targetTree.type;
            replacementTree.operator = targetTree.operator;
        }

        // Return result in some magical way...
        return replacementTree;
    }

    @Override
    public void visitBinary(JCBinary tree) {
        if (tree.getTag() != currentSubtreeTag) {
            // If it's not a continuation of the commutative subtree, it's a leaf thereof...
            combinedElements.add(tree);
            return;
        }

        // Even if it is, check if this is secretly string concatenation...
        if (currentSubtreeTag == Tag.PLUS) {
            log.info("Node: {}", tree.lhs);
            log.info("Node type: {}", tree.lhs.type);
            log.info("Node type: {}", tree.rhs.type);
            if (tree.lhs.type.toString().equals("java.lang.String")
             || tree.rhs.type.toString().equals("java.lang.String")) {
                log.info("String concat!");
                combinedElements.add(tree);
                return;
            }
        }

        super.visitBinary(tree);
    }

    @Override
    public void visitUnary(JCUnary tree) {
        combinedElements.add(tree);
    }

    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        combinedElements.add(jcMethodInvocation);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        combinedElements.add(jcIdent);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        combinedElements.add(jcFieldAccess);
    }

    @Override
    public void visitTypeTest(JCInstanceOf jcInstanceOf) {
        combinedElements.add(jcInstanceOf);
    }

    @Override
    public void visitLiteral(JCLiteral jcLiteral) {
        combinedElements.add(jcLiteral);
    }

    @Override
    public void visitTypeCast(JCTypeCast jcTypeCast) {
        combinedElements.add(jcTypeCast);
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        combinedElements.add(jcAssignOp);
    }

    @Override
    public void visitAssign(JCAssign jcAssign) {
        combinedElements.add(jcAssign);
    }

    @Override
    public void visitConditional(JCConditional jcConditional) {
        combinedElements.add(jcConditional);
    }
}
