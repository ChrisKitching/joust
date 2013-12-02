package tests.unittests;

import static com.sun.tools.javac.tree.JCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.constfold.BaseConstFoldTest.*;

import com.sun.tools.javac.tree.JCTree;
import joust.translators.ConstFoldTranslator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the constant folder.
 * Generates a set of parse tree fragments and ensures the results of constant folding on them are
 * as expected.
 * Due to the JUnitParamsRunner, test X gets its param set from a method called parametersForX.
 */
@RunWith(JUnitParamsRunner.class)
public @Log4j2
class ConstFoldTranslatorTest extends BaseTreeTranslatorTest<ConstFoldTranslator> {
    public ConstFoldTranslatorTest() {
        super(ConstFoldTranslator.class);
    }

    @Override
    @Test
    @Parameters(method = "unaryArgs")
    public void testVisitUnary(JCUnary input, JCTree expected) throws Exception {
        super.testVisitUnary(input, expected);
    }

    @Override
    @Test
    @Parameters(method = "binaryArgs")
    public void testVisitBinary(JCBinary input, JCTree expected) throws Exception {
        super.testVisitBinary(input, expected);
    }

    @Override
    @Test
    @Parameters(method = "parensArgs")
    public void testVisitParens(JCParens input, JCTree expected) throws Exception {
        super.testVisitParens(input, expected);
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

            $(plus(l(1), l(1)), l(2)),
            $(plus(l('a'), l('c')), l('a' + 'c')),
            $(plus(l(Double.NaN), l(Integer.MAX_VALUE)), l(Double.NaN + Integer.MAX_VALUE)),

            $(mul(parens(urShift(l(2), l('t'))), l(6L)), l(6L * (2 >>> 't'))),
            $(eq(lt(div(l(8), l(2)), l(4)), or(l(true), l(false))), l(((8 / 2) < 4) == (true || false)))
        );
    }

    public Object[] parensArgs() {
        return
        $(
            $(parens(l(42)), l(42)),
            $(parens(parens(plus(l(42), l(52)))), l(94))
        );
    }
}
