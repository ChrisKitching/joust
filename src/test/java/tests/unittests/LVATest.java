package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.optimisers.visitors.Live;
import joust.utils.LogUtils;
import joust.utils.SymbolSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static  com.sun.tools.javac.code.Symbol.*;
import static tests.unittests.utils.UnitTestTreeFactory.*;

/**
 * Unit tests for live variable analysis.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public class LVATest extends BaseAnalyserTest<Live> {

    public LVATest() {
        super(Live.class, "liveVariables");
    }

    /**
     * Run LVA over inputTree and verify that the live set at targetNode subsequently becomes expectedLive.
     */
    @Test
    @Parameters(method = "lvaArgs")
    public void testLVA(AJCMethodDecl inputTree, Object[] targetNodes, Object[] expectedLives) {
        SymbolSet[] symbolSets = new SymbolSet[expectedLives.length];

        for (int i = 0; i < expectedLives.length; i++) {
            Object[] symbols = (Object[]) expectedLives[i];
            SymbolSet expected = new SymbolSet();
            for (int b = 0; b < symbols.length; b++) {
                expected.add((VarSymbol) symbols[b]);
            }

            symbolSets[i] = expected;
        }

        testVisitNode(inputTree, targetNodes, symbolSets);
    }

    @SuppressWarnings("unchecked")
    public Object[] lvaArgs() {
        final Name xName = NameFactory.getName();
        final Name yName = NameFactory.getName();
        final Name zName = NameFactory.getName();

        // Declaration nodes for three local variables.
        AJCVariableDecl xEqThree = local(xName, Int(), l(3));  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl yEqFour = local(yName, Int(), l(4));
        VarSymbol ySym = yEqFour.getTargetSymbol();

        AJCVariableDecl zDecl = local(zName, Int(), plus(Ident(xSym), Ident(xSym)));

        AJCAssign xAsgSeven = Assign(Ident(xSym), l(7));      // x = 7;

        AJCBinary yGtFour = gt(Ident(ySym), l(4));  // y > 4

        /*
        if (y > 4) {
            x = 7;
        } else {
            x = 8;
            y = 9;
        }
         */
        AJCAssign xEqEight = Assign(Ident(xSym), l(8));
        AJCIf anIf = If(yGtFour, Block(Assign(Ident(xSym), l(7))),
                                 Block(xEqEight, Assign(Ident(ySym), l(9))));


        AJCVariableDecl yEqThree = local(xName, Int(), l(3));  // int x = 3;

        AJCBinary xGtFive = gt(Ident(xSym), l(5));
        AJCUnaryAsg xPlusPlus = postInc(Ident(xSym));

        AJCForLoop emptyFor = ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(Exec(xPlusPlus)), Block(0, List.<AJCStatement>nil()));

        return
        $(
                /*
                int x = 3;   <-- Target
                x = 7;       <-- Target
                int z = x + x;
                 */
                $(
                     MethodFromBlock(Block(xEqThree, xAsgSeven, zDecl)),
                      $(xEqThree, xAsgSeven),
                      $($v(), $v(xSym))
                ),

                /*
                int y = 4;
                int x = 3;   <-- Target
                if (y > 4) {
                    x = 7;
                } else {
                    x = 8;   <-- Target
                    y = 9;
                }
                 */
                $(
                     MethodFromBlock(Block(yEqFour, xEqThree, anIf)),
                      $(xEqThree, xEqEight),
                      $($v(ySym), $v())
                ),

                /*

                for (int i = 0;   <-- Target
                    i < 10;
                    i++)          <-- Target
                    {}
                 */
                $(
                    MethodFromBlock(Block(emptyFor)),
                     $(xEqThree, xPlusPlus),
                     $($v(xSym), $v())
                ),

                /*
                int x = 3;
                x++;         <-- Target
                 */
                $(
                    MethodFromBlock(Block(xEqThree, xPlusPlus)),
                     $(xPlusPlus, xEqThree),
                     $($v(), $($v(xSym)))
                )
        );
    }
}
