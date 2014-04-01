package tests.unittests;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.NameFactory;
import joust.optimisers.visitors.Live;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import joust.utils.LogUtils;
import joust.utils.SymbolSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.text.html.HTML;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static  com.sun.tools.javac.code.Symbol.*;
import static tests.unittests.utils.UnitTestTreeFactory.*;
import static joust.utils.StaticCompilerUtils.treeCopier;

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

        SideEffectVisitor effects = new SideEffectVisitor();
        effects.visitMethodDef(inputTree);

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
        VarSymbol zSym = zDecl.getTargetSymbol();

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


        AJCBinary xGtFive = gt(Ident(xSym), l(5));
        AJCUnaryAsg xPlusPlus = postInc(Ident(xSym));

        AJCForLoop emptyFor = ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(Exec(xPlusPlus)), Block(0, List.<AJCStatement>nil()));



        AJCVariableDecl zDeclThree = local(zName, Int(), l(3));
        AJCVariableDecl yDeclThree = local(yName, Int(), l(3));

        AJCAssign yAsgOne = Assign(Ident(ySym), l(1));
        AJCAssignOp zPlusEqualsI = Assignop(JCTree.Tag.PLUS_ASG, Ident(zSym), Ident(xSym));
        AJCBlock forBlock = Block(yAsgOne,  callFor(zSym), zPlusEqualsI);
        AJCForLoop nonemptyFor = ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(Exec(xPlusPlus)), forBlock);


        AJCAssign yEqX = Assign(Ident(ySym), Ident(xSym));
        AJCForLoop yxFor = ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(Exec(xPlusPlus)), Block(yEqX));


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
                    i < 5;
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
                ),

                /*
                int z = 3;
                int y = 3;      <-- Target
                for (int x = 3; x < 5; x++) {
                    y = 1;      <-- Target
                    f(z);
                    z += x      <-- Target
                }
                 */
                $(
                     MethodFromBlock(Block(zDeclThree, yDeclThree, nonemptyFor)),
                       $(yDeclThree, yAsgOne, zPlusEqualsI),
                       $($v(zSym, xSym), $v(zSym, xSym), $v(zSym, xSym))
                ),


                /*
                int y = 3;      <-- Target
                for (int x = 3; x < 5; x++) {
                    y = x;      <-- Target
                }
                f(y);
                 */
                $(
                    MethodFromBlock(Block(yDeclThree, yxFor, callFor(ySym))),
                      $(yDeclThree, yEqX),
                      $($v(xSym, ySym), $v(xSym, ySym))
                )
        );
    }
}
