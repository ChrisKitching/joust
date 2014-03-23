package joust.optimisers.invar;

import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.treeinfo.EffectSet;
import joust.utils.SymbolSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Set;

import static joust.tree.annotatedtree.AJCTree.*;
import static com.sun.tools.javac.code.Symbol.*;

@Log4j2
@RequiredArgsConstructor
public class InvariantExpressionFinder extends AJCTreeVisitor {
    public final Set<AJCExpressionTree> invariantExpressions = new HashSet<>();

    // Symbols invalidated in the loop of interest - passed by the caller.
    @NonNull private final Set<VarSymbol> writtenInLoop;

    private void addIfInvariant(AJCExpressionTree that) {
        log.debug("Considering invariance of {}", that);
        EffectSet exprEffects = that.effects.getEffectSet();
        log.debug("Effects: {}", exprEffects);

        // Escaping symbol uses are omitted to avoid concurrency problems.
        // Write effects cause something to be omitted from moving out of the loop.
        if (exprEffects.contains(EffectSet.EffectType.READ_ESCAPING)
         || exprEffects.contains(EffectSet.EffectType.WRITE_INTERNAL)
         || exprEffects.contains(EffectSet.EffectType.WRITE_ESCAPING)) {
            log.debug("No good - contains unacceptable writes of escaping reads.");
            return;
        }

        if (exprEffects.contains(EffectSet.EffectType.READ_INTERNAL)) {
            // Determine if this expression reads any symbols that are written in the loop.
            SymbolSet readSymbols = new SymbolSet(exprEffects.readInternal);

            log.debug("ReadSymbols: {}", readSymbols);
            readSymbols.retainAll(writtenInLoop);
            log.debug("After dropping: {}", readSymbols);

            if (!readSymbols.isEmpty()) {
                log.debug("No good - reads symbols written in the loop.");
                return;
            }
        }

        invariantExpressions.add(that);
    }

    @Override
    protected void visitBinary(AJCBinary that) {
        addIfInvariant(that);

        super.visitBinary(that);
    }

    @Override
    protected void visitTypeCast(AJCTypeCast that) {
        addIfInvariant(that);

        super.visitTypeCast(that);
    }

    @Override
    protected void visitInstanceOf(AJCInstanceOf that) {
        addIfInvariant(that);

        super.visitInstanceOf(that);
    }

    @Override
    protected void visitUnary(AJCUnary that) {
        addIfInvariant(that);

        super.visitUnary(that);
    }
}
