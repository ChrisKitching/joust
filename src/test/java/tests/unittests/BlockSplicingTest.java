package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.NameFactory;
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
import static com.sun.tools.javac.code.Symbol.*;
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
        AJCVariableDecl xEqThree = f.local(xName, f.Int(), f.l(3));  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl yEqFour = f.local(yName, f.Int(), f.l(4));
        VarSymbol ySym = yEqFour.getTargetSymbol();

        AJCVariableDecl zDecl = f.local(zName, f.Int(), f.plus(f.Ident(xSym), f.Ident(xSym)));

        AJCExpressionStatement xAsgSeven = f.Exec(f.Assign(f.Ident(xSym), f.l(7)));      // x = 7;

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


        return
        $(
            $(
                f.Block(xEqThree, zDecl),
                List.of(xAsgSeven),
                1,
                f.Block(xEqThree, xAsgSeven, zDecl)
            ),

            $(
                f.Block(xEqThree, zDecl, emptyFor),
                List.of(anIf, yEqFour),
                3,
                f.Block(xEqThree, zDecl, emptyFor, anIf, yEqFour)
            ),

            $(
                f.Block(xEqThree, zDecl, emptyFor),
                List.of(anIf, yEqFour),
                2,
                f.Block(xEqThree, zDecl, anIf, yEqFour, emptyFor)
            )
        );
    }
}
