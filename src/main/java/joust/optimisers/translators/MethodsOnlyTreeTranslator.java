package joust.optimisers.translators;

import static joust.tree.annotatedtree.AJCTree.*;

public class MethodsOnlyTreeTranslator extends BaseTranslator {
    @Override
    public void visitClassDef(AJCClassDecl tree) {
        visit(tree.methods);
    }
}
