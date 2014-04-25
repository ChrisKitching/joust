package joust.optimisers.normalise;

import joust.optimisers.translators.BaseTranslator;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.logging.LogUtils;
import joust.utils.tree.TreeUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public class TreeNormalisingTranslator extends BaseTranslator {
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
