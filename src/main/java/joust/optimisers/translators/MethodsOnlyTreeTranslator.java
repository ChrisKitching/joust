package joust.optimisers.translators;

import com.sun.tools.javac.tree.JCTree;

public class MethodsOnlyTreeTranslator extends BaseTranslator {
    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        for (JCTree t : tree.defs) {
            // WHYYYY JAVAC!? WHY SO NO USE TYPE SYSTEM?!
            if (t instanceof JCTree.JCMethodDecl) {
                visitMethodDef((JCTree.JCMethodDecl) t);
            }
        }
    }
}
