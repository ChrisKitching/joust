package joust.optimisers.visitors;

import joust.optimisers.avail.normalisedexpressions.PossibleSymbol;
import joust.utils.TreeUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.tree.JCTree.*;

/**
 * A tree visitor to determine the set of VarSymbols a given subtree kills. Used in loop invariant code motion.
 */
public @Log4j2 class KillSetVisitor extends DepthFirstTreeVisitor {
    public HashSet<PossibleSymbol> killSet = new HashSet<>();

    @Override
    public void visitAssign(JCAssign jcAssign) {
        super.visitAssign(jcAssign);

        killSet.add(PossibleSymbol.getConcrete(TreeUtils.getTargetSymbolForExpression(jcAssign.lhs)));
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        killSet.add(PossibleSymbol.getConcrete(TreeUtils.getTargetSymbolForExpression(jcAssignOp.lhs)));
    }

    @Override
    public void visitUnary(JCUnary jcUnary) {
        super.visitUnary(jcUnary);

        // If the unary is the sort that has evil side effects...
        Tag opcode = jcUnary.getTag();
        if (opcode == Tag.PREINC
         || opcode == Tag.PREDEC
         || opcode == Tag.POSTINC
         || opcode == Tag.POSTDEC) {
            killSet.add(PossibleSymbol.getConcrete(TreeUtils.getTargetSymbolForExpression(jcUnary.getExpression())));
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);

        // A definition kills the symbol - will detect loop-local variables and suchlike.
        killSet.add(PossibleSymbol.getConcrete(jcVariableDecl.sym));
    }
}
