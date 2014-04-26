package tests.unittests;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.NameFactory;
import joust.analysers.Live;
import joust.analysers.sideeffects.SideEffectVisitor;
import joust.utils.logging.LogUtils;
import joust.utils.data.SymbolSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
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

        SideEffectVisitor effects = new SideEffectVisitor(true);
        effects.visitMethodDef(inputTree);

        testVisitNode(inputTree, targetNodes, symbolSets);
    }

    @SuppressWarnings("unchecked")
    public Object[] lvaArgs() {
        final Name xName = NameFactory.getName();
        final Name yName = NameFactory.getName();
        final Name zName = NameFactory.getName();

        // Declaration nodes for three local variables.
        AJCVariableDecl xEqThree = f.local(xName, f.Int(), f.l(3));  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl yEqFour = f.local(yName, f.Int(), f.l(4));
        VarSymbol ySym = yEqFour.getTargetSymbol();

        AJCVariableDecl zDecl = f.local(zName, f.Int(), f.plus(f.Ident(xSym), f.Ident(xSym)));
        VarSymbol zSym = zDecl.getTargetSymbol();

        AJCAssign xAsgSeven = f.Assign(f.Ident(xSym), f.l(7));      // x = 7;

        AJCBinary yGtFour = f.gt(f.Ident(ySym), f.l(4));  // y > 4

        /*
        if (y > 4) {
            x = 7;
        } else {
            x = 8;
            y = 9;
        }
         */
        AJCAssign xEqEight = f.Assign(f.Ident(xSym), f.l(8));
        AJCIf anIf = f.If(yGtFour, f.Block(f.Assign(f.Ident(xSym), f.l(7))),
                                   f.Block(xEqEight, f.Assign(f.Ident(ySym), f.l(9))));


        AJCBinary xGtFive = f.gt(f.Ident(xSym), f.l(5));
        AJCUnaryAsg xPlusPlus = f.postInc(f.Ident(xSym));

        AJCForLoop emptyFor = f.ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(f.Exec(xPlusPlus)), f.Block(0, List.<AJCStatement>nil()));



        AJCVariableDecl zDeclThree = f.local(zName, f.Int(), f.l(3));
        AJCVariableDecl yDeclThree = f.local(yName, f.Int(), f.l(3));

        AJCAssign yAsgOne = f.Assign(f.Ident(ySym), f.l(1));
        AJCAssignOp zPlusEqualsI = f.Assignop(JCTree.Tag.PLUS_ASG, f.Ident(zSym), f.Ident(xSym));
        AJCBlock forBlock = f.Block(yAsgOne,  f.callFor(zSym), zPlusEqualsI);
        AJCForLoop nonemptyFor = f.ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(f.Exec(xPlusPlus)), forBlock);


        AJCAssign yEqX = f.Assign(f.Ident(ySym), f.Ident(xSym));
        AJCForLoop yxFor = f.ForLoop(List.<AJCStatement>of(xEqThree), xGtFive, List.of(f.Exec(xPlusPlus)), f.Block(yEqX));


        return
        $(
                /*
                int x = 3;   <-- Target
                x = 7;       <-- Target
                int z = x + x;
                 */
                $(
                    f.MethodFromBlock(f.Block(xEqThree, xAsgSeven, zDecl)),
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
                    f.MethodFromBlock(f.Block(yEqFour, xEqThree, anIf)),
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
                    f.MethodFromBlock(f.Block(emptyFor)),
                     $(xEqThree, xPlusPlus),
                     $($v(xSym), $v())
                ),

                /*
                int x = 3;
                x++;         <-- Target
                 */
                $(
                    f.MethodFromBlock(f.Block(xEqThree, xPlusPlus)),
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
                    f.MethodFromBlock(f.Block(zDeclThree, yDeclThree, nonemptyFor)),
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
                    f.MethodFromBlock(f.Block(yDeclThree, yxFor, f.callFor(ySym))),
                      $(yDeclThree, yEqX),
                      $($v(xSym, ySym), $v(xSym, ySym))
                )
        );
    }
}
