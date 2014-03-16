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

    // For SCIENCE

    @Override
    public void visitIdent(JCTree.JCIdent jcIdent) {
        log.trace("Visit ident: "+jcIdent);
        log.trace("Name: "+jcIdent.name);
        log.trace("Sym: "+jcIdent.sym);
        log.trace("Sym: "+jcIdent.sym.getClass().getSimpleName());
        super.visitIdent(jcIdent);

    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess jcFieldAccess) {
        log.trace("Visit field access: "+jcFieldAccess);
        log.trace("selected:" +jcFieldAccess.selected);
        log.trace("name:" +jcFieldAccess.name);
        log.trace("sym:" +jcFieldAccess.sym);
        log.trace("sym:" +jcFieldAccess.sym.getClass().getSimpleName());
        super.visitSelect(jcFieldAccess);
    }

    @Override
    public void visitReference(JCTree.JCMemberReference jcMemberReference) {
        log.trace("Visit member ref: "+jcMemberReference);
        super.visitReference(jcMemberReference);
    }
}
