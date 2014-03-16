package joust.optimisers.translators;

import joust.tree.annotatedtree.AJCTree;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import static joust.tree.annotatedtree.AJCTree.*;

@Log4j2
public class ExpressionNormalisingTranslator extends BaseTranslator {
    @Override
    public void visitBinary(AJCBinary binary) {
        super.visitBinary(binary);
        if (TreeUtils.operatorIsCommutative(binary.getTag())) {
            CommutativitySorter sorter = new CommutativitySorter(binary);
            log.debug("Commutativity sorter running on: {}", binary);
            AJCTree result = sorter.process();
            binary.swapFor(result);

            log.debug("Commutativity sorter returned: {}", result);
        }
    }
}
