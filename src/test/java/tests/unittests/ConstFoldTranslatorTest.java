package tests.unittests;

import static com.sun.tools.javac.tree.JCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.constfold.BaseConstFoldTest.*;
import static org.junit.Assert.*;

import com.sun.tools.javac.tree.JCTree;
import joust.translators.ConstFoldTranslator;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import tests.unittests.TreeFabricatingTest;

/**
 * Test for the constant folder.
 * Generates a set of parse tree fragments and ensures the results of constant folding on them are
 * as expected.
 * Due to the JUnitParamsRunner, test X gets its param set from a method called parametersForX.
 */
@RunWith(JUnitParamsRunner.class)
public class ConstFoldTranslatorTest extends TreeFabricatingTest {
    private static Logger logger = LogManager.getLogger();

    @Test
    @Parameters(method = "unaryArgs")
    public void testVisitUnary(JCTree input, JCTree expected) throws Exception {
        ConstFoldTranslator constFold = new ConstFoldTranslator();
        constFold.visitUnary((JCUnary) input);
        logger.info("Result: {}  Expected: {}", constFold.getResult(), expected);
        // For these sorts of trees, equality can safely be determined by comparing the results of
        // toString without the need to write a comparator.
        // This may not hold for all tree types - it may subsequently prove necessary to write a
        // proper comparator of trees.
        assertEquals(expected.toString(), constFold.getResult().toString());
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

    @Test
    @Parameters(method = "binaryArgs")
    public void testVisitBinary(JCTree input, JCTree expected) throws Exception {
        ConstFoldTranslator constFold = new ConstFoldTranslator();
        constFold.visitBinary((JCBinary) input);
        logger.info("Result: {}  Expected: {}", constFold.getResult(), expected);
        // For these sorts of trees, equality can safely be determined by comparing the results of
        // toString without the need to write a comparator.
        // This may not hold for all tree types - it may subsequently prove necessary to write a
        // proper comparator of trees.
        assertEquals(expected.toString(), constFold.getResult().toString());
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

    @Test
    @Parameters(method = "parensArgs")
    public void testVisitParens(JCParens input, JCTree expected) throws Exception {
        ConstFoldTranslator constFold = new ConstFoldTranslator();
        logger.debug("Input tree: {} Expected: {}", input, expected);
        constFold.visitParens(input);
        logger.debug("Result: {}  Expected: {}", constFold.getResult(), expected);
        // For these sorts of trees, equality can safely be determined by comparing the results of
        // toString without the need to write a comparator.
        // This may not hold for all tree types - it may subsequently prove necessary to write a
        // proper comparator of trees.
        assertEquals(expected.toString(), constFold.getResult().toString());
    }

    public Object[] parensArgs() {
        return
        $(
            $(parens(l(42)), l(42)),
            $(parens(parens(plus(l(42), l(52)))), l(94))
        );
    }
}
