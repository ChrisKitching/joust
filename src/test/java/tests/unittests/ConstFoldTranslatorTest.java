package tests.unittests;

import joust.optimisers.translators.ConstFoldTranslator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static joust.utils.StaticCompilerUtils.treeMaker;

/**
 * Test for the constant folder.
 * Generates a set of parse tree fragments and ensures the results of constant folding on them are
 * as expected.
 * Due to the JUnitParamsRunner, test X gets its param set from a method called parametersForX.
 */
@Log4j2
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
            $(neg(l(1)), l(-1)),
            $(neg(l(0.0)), l(-0.0)),
            $(neg(l(Double.NaN)), l(Double.NaN)),
            $(comp(l(0xDEADBEEF)), l(~0xDEADBEEF)),
            $(not(l(false)), l(true)),
            $(not(l(true)), l(false)),
            $(pos(l(56.3)), l(56.3))
        );
    }

    public Object[] binaryArgs() {
        return
        $(
            $(bitOr(l(0xABCDE), l(0xFACE)), l(0xABCDE | 0xFACE)),
            $(bitOr(l('c'), l(Long.MAX_VALUE)), l('c' | Long.MAX_VALUE)),
            $(bitOr(l(0), l(1)), l(1)),

            $(lShift(l(2), l(4)), l(2 << 4)),
            $(lShift(l(5), l(6L)), l(5 << 6L)),
            $(lShift(l('t'), l(1)), l('t' << 1)),

            $(lShift(l(2), l(4)), l(2 << 4)),
            $(lShift(l(5), l(6L)), l(5 << 6L)),
            $(lShift(l('t'), l(1)), l('t' << 1)),

            $(plus(l("cake"), l("tacular")), l("caketacular")),

            $(plus(l(1), l(1)), l(2)),
            $(plus(l('a'), l('c')), l('a' + 'c')),
            $(plus(l(Double.NaN), l(Integer.MAX_VALUE)), l(Double.NaN + Integer.MAX_VALUE)),

            $(mul(urShift(l(2), l('t')), l(6L)), l(6L * (2 >>> 't'))),
            $(eq(lt(div(l(8), l(2)), l(4)), or(l(true), l(false))), l(((8 / 2) < 4) == (true || false)))
        );
    }
}
