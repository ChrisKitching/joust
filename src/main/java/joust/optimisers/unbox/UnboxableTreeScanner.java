package joust.optimisers.unbox;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import joust.tree.annotatedtree.AJCTreeVisitor;

import java.util.HashSet;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static joust.utils.compiler.StaticCompilerUtils.*;

public class UnboxableTreeScanner extends AJCTreeVisitor {
    public Set<VarSymbol> toucheBoxingSymbols = new HashSet<VarSymbol>();

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



    /**
     * If a reference is to a symbol of a boxing type, add it to the toucheBoxingSymbols set.
     */
    private void processRef(AJCSymbolRefTree that) {
        Symbol target = that.getTargetSymbol();
        if (target instanceof VarSymbol) {
            if (types.unboxedType(target.type) != Type.noType) {
                toucheBoxingSymbols.add((VarSymbol) target);
            }
        }
    }
}
