package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.NameFactory;
import joust.tree.annotatedtree.AJCTree;
import joust.utils.data.JavacListUtils;
import joust.utils.logging.LogUtils;
import joust.utils.ReflectionUtils;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Logger;

import static joust.utils.compiler.StaticCompilerUtils.*;
import static org.junit.Assert.*;
import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static com.sun.tools.javac.code.Symbol.*;
import static tests.unittests.utils.UnitTestTreeFactory.*;

/**
 * A test of the tree swapFor mechanism. (I so very much do not trust it, and it is sort of important).
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public class SwapForTest extends TreeFabricatingTest {
    @Test
    @Parameters(method = "swapArgs")
    @SuppressWarnings("unchecked")
    public void testSwapField(AJCTree parentTree, AJCTree oldTree, AJCTree newTree, String testFieldName) {
        log.debug("Swapping {}:{}\n for\n {}:{}\n in {}{}.", oldTree, oldTree.getClass().getCanonicalName(),
                                                             newTree, newTree.getClass().getCanonicalName(),
                                                             parentTree, parentTree.getClass().getCanonicalName());

        // Check the desired field for successful swappage
        try {
            Field f = ReflectionUtils.findField(parentTree.getClass(), testFieldName);
            f.setAccessible(true);

            Object value = f.get(parentTree);

            int index = -1;
            List<Object> listVal;
            boolean isListSwap = "com.sun.tools.javac.util.List".equals(f.getType().getCanonicalName());
            if (isListSwap) {
                // The original list.
                listVal = (List<Object>) value;
                assertTrue(listVal != null);

                index = JavacListUtils.dumbIndexOf(listVal, oldTree);
                if (index == -1) {
                    log.error("Unable to find {} in {}", oldTree, Arrays.toString(listVal.toArray()));
                    fail();
                }
            }

            oldTree.swapFor(newTree);

            if (isListSwap) {
                listVal = (List<Object>) f.get(parentTree);

                value = listVal.get(index);
            } else {
                value = f.get(parentTree);
            }

            if (value != newTree) {
                log.error("Expecting:\n{}", newTree);
                log.error("Found:\n{}", value);
            } else {
                log.debug("Expecting:\n{}", newTree);
                log.debug("Found:\n{}", value);
            }

            assertTrue(value == newTree);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            fail(e.toString());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public Object[] swapArgs() {
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


        MethodSymbol fakeMethod = f.virtualMethod();

        AJCIdent xIdent = f.Ident(xSym);
        AJCIdent yIdent = f.Ident(ySym);
        AJCIdent zIdent = f.Ident(zSym);

        AJCCall call = f.Call(f.Ident(fakeMethod), List.<AJCExpressionTree>of(xIdent, yIdent));


        AJCVariableDecl yQFourCopy = treeCopier.copy(yEqFour);


        AJCExpressionStatement xAsgSeven = f.Exec(f.Assign(f.Ident(xSym), f.l(7)));      // x = 7

        AJCBinary yGtFour = f.gt(f.Ident(ySym), f.l(4));  // y > 4

        /*
        if (y > 4) {
            x = 7;
        } else {
            x = 8;
            y = 9;
        }
         */
        AJCExpressionStatement xEqEight = f.Exec(f.Assign(f.Ident(xSym), f.l(8)));
        AJCIf anIf = f.If(yGtFour, f.Block(xAsgSeven),
                f.Block(xEqEight, f.Exec(f.Assign(f.Ident(ySym), f.l(9)))));

        return
        $(
            $(
                call,
                xIdent,
                zIdent,
                "args"
            ),

            $(
                yQFourCopy,
                yQFourCopy.getInit(),
                treeMaker.EmptyExpression(),
                "init"
            ),

            $(
                anIf.elsepart,
                xEqEight,
                treeCopier.copy(xAsgSeven),
                "stats"
            )
        );
    }
}
