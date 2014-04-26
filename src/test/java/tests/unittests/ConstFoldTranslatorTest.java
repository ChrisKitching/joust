package tests.unittests;

import joust.optimisers.translators.ConstFoldTranslator;
import joust.utils.logging.LogUtils;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static joust.utils.compiler.StaticCompilerUtils.treeMaker;

/**
 * Test for the constant folder.
 * Generates a set of parse tree fragments and ensures the results of constant folding on them are
 * as expected.
 * Due to the JUnitParamsRunner, test X gets its param set from a method called parametersForX.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public
class ConstFoldTranslatorTest extends BaseTreeTranslatorTest<ConstFoldTranslator> {
    public ConstFoldTranslatorTest() {
        super(ConstFoldTranslator.class);
    }

    @Test
    @Parameters(method = "unaryArgs")
    public void testVisitUnary(AJCUnary input, AJCExpressionTree expected) {
        testVisitNode(treeMaker.Exec(input), treeMaker.Exec(expected));
    }

    @Test
    @Parameters(method = "binaryArgs")
    public void testVisitBinary(AJCBinary input, AJCExpressionTree expected) {
        testVisitNode(treeMaker.Exec(input), treeMaker.Exec(expected));
    }

    public Object[] unaryArgs() {
        return
        $(
            $(f.neg(f.l(1)), f.l(-1)),
            $(f.neg(f.l(0.0)), f.l(-0.0)),
            $(f.neg(f.l(Double.NaN)), f.l(Double.NaN)),
            $(f.comp(f.l(0xDEADBEEF)), f.l(~0xDEADBEEF)),
            $(f.not(f.l(false)), f.l(true)),
            $(f.not(f.l(true)), f.l(false)),
            $(f.pos(f.l(56.3)), f.l(56.3))
        );
    }

    public Object[] binaryArgs() {
        return
        $(
            $(f.bitOr(f.l(0xABCDE), f.l(0xFACE)), f.l(0xABCDE | 0xFACE)),
            $(f.bitOr(f.l('c'), f.l(Long.MAX_VALUE)), f.l('c' | Long.MAX_VALUE)),
            $(f.bitOr(f.l(0), f.l(1)), f.l(1)),

            $(f.lShift(f.l(2), f.l(4)), f.l(2 << 4)),
            $(f.lShift(f.l(5), f.l(6L)), f.l(5 << 6L)),
            $(f.lShift(f.l('t'), f.l(1)), f.l('t' << 1)),

            $(f.lShift(f.l(2), f.l(4)), f.l(2 << 4)),
            $(f.lShift(f.l(5), f.l(6L)), f.l(5 << 6L)),
            $(f.lShift(f.l('t'), f.l(1)), f.l('t' << 1)),

            $(f.plus(f.l("cake"), f.l("tacular")), f.l("caketacular")),

            $(f.plus(f.l(1), f.l(1)), f.l(2)),
            $(f.plus(f.l('a'), f.l('c')), f.l('a' + 'c')),
            $(f.plus(f.l(Double.NaN), f.l(Integer.MAX_VALUE)), f.l(Double.NaN + Integer.MAX_VALUE)),

            $(f.mul(f.urShift(f.l(2), f.l('t')), f.l(6L)), f.l(6L * (2 >>> 't'))),
            $(f.eq(f.lt(f.div(f.l(8), f.l(2)), f.l(4)), f.or(f.l(true), f.l(false))), f.l(((8 / 2) < 4) == (true || false)))
        );
    }
}
