package joust.analysers;

import com.sun.tools.javac.code.Symbol;
import joust.tree.annotatedtree.AJCTreeVisitor;

import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;

public class TouchedSymbolLocator extends AJCTreeVisitor {
    Set<VarSymbol> touched = new HashSet<>();

    @Override
    protected void visitAssign(AJCAssign that) {
        visit(that.rhs);
    }

    @Override
    protected void visitFieldAccess(AJCFieldAccess that) {
        super.visitFieldAccess(that);
        processRef(that);
    }

    @Override
    protected void visitIdent(AJCIdent that) {
        super.visitIdent(that);
        processRef(that);
    }

    private void processRef(AJCSymbolRefTree that) {

        Symbol target = that.getTargetSymbol();
        if (target instanceof VarSymbol) {
            touched.add((VarSymbol) target);
        }
    }
}
