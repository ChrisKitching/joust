package joust.optimisers.visitors;

import joust.tree.annotatedtree.AJCTreeVisitor;
import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.code.Symbol.*;
import static joust.tree.annotatedtree.AJCTree.*;

/**
 * Visitor that fetches the list of VarSymbols declared in the given tree.
 */
public class AssignmentLocator extends AJCTreeVisitor {
    Set<VarSymbol> declarations = new HashSet<>();

    @Override
    protected void visitVariableDecl(AJCVariableDecl that) {
        super.visitVariableDecl(that);

        declarations.add(that.getTargetSymbol());
    }
}
