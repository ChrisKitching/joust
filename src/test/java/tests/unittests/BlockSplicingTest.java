package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.NameFactory;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.treeinfo.TreeInfoManager;
import joust.utils.LogUtils;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static com.sun.tools.javac.code.Symbol.*;
import static tests.unittests.utils.UnitTestTreeFactory.*;
import static org.junit.Assert.*;

/**
 * Unit tests for AJCBlock's block splicing utilities.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public class BlockSplicingTest extends TreeFabricatingTest {
    @Test
    @Parameters(method = "splicingArgs")
    public void testBlockSplicing(AJCBlock inBlock, List<AJCStatement> splicedStatements, int insertionIndex, AJCBlock expected) {
        log.debug("Splicing {}\n into\n {}\n at {}", splicedStatements, inBlock, insertionIndex);
        inBlock.insert(splicedStatements, insertionIndex);
        log.debug("Expecting:\n{}\nFound:\n{}", expected.toString(), inBlock.toString());
        assertTrue(inBlock.toString().equals(expected.toString()));
    }

    @SuppressWarnings("unchecked")
    public Object[] splicingArgs() {
        final Name xName = NameFactory.getName();
        final Name yName = NameFactory.getName();
        final Name zName = NameFactory.getName();

        // Declaration nodes for three local variables.
        AJCVariableDecl xEqThree = local(xName, Int(), l(3));  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl yEqFour = local(yName, Int(), l(4));
        VarSymbol ySym = yEqFour.getTargetSymbol();

        AJCVariableDecl zDecl = local(zName, Int(), plus(Ident(xSym), Ident(xSym)));

        AJCExpressionStatement xAsgSeven = Exec(Assign(Ident(xSym), l(7)));      // x = 7;

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
            $(
                Block(xEqThree, zDecl),
                List.of(xAsgSeven),
                1,
                Block(xEqThree, xAsgSeven, zDecl)
            ),

            $(
                Block(xEqThree, zDecl, emptyFor),
                List.of(anIf, yEqFour),
                3,
                Block(xEqThree, zDecl, emptyFor, anIf, yEqFour)
            ),

            $(
                Block(xEqThree, zDecl, emptyFor),
                List.of(anIf, yEqFour),
                2,
                Block(xEqThree, zDecl, anIf, yEqFour, emptyFor)
            )
        );
    }
}
