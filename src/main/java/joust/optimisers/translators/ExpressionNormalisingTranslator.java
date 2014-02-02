package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

public @Log4j2 class ExpressionNormalisingTranslator extends BaseTranslator {
    @Override
    public void visitBinary(JCTree.JCBinary jcBinary) {
        super.visitBinary(jcBinary);
        if (TreeUtils.operatorIsCommutative(jcBinary.getTag())) {
            CommutativitySorter sorter = new CommutativitySorter(jcBinary);
            log.debug("Commutativity sorter running on: {}", jcBinary);
            result = sorter.process();

            log.debug("Commutativity sorter returned: {}", result);
        }
    }


    @Override
    public void visitMethodDef(JCTree.JCMethodDecl tree) {
        log.info("Unnormalised expression tree:\n {}", tree);
        super.visitMethodDef(tree);
        log.info("Normalised expression tree:\n {}", tree);
    }
}
