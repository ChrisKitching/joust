package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.NameFactory;
import joust.optimisers.translators.UnusedAssignmentStripper;
import joust.analysers.sideeffects.SideEffectVisitor;
import joust.utils.logging.LogUtils;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static  com.sun.tools.javac.code.Symbol.*;
import static joust.utils.compiler.StaticCompilerUtils.treeCopier;

/**
 * Some unit tests for the unused assignment stripper.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public class UnusedAssignmentStripperTest extends BaseTreeTranslatorTest<UnusedAssignmentStripper> {
    public UnusedAssignmentStripperTest() {
        super(UnusedAssignmentStripper.class);
    }

    @Ignore
    @Test
    @Parameters(method = "unusedArgs")
    public void testUnused(AJCMethodDecl input, AJCMethodDecl expected) {
        SideEffectVisitor effects = new SideEffectVisitor(true);
        effects.visitMethodDef(input);

        log.debug("Unused assignment strip for: {}", input);
        testVisitNodeBluntForce(input, expected);
    }

    @SuppressWarnings("unchecked")
    public Object[] unusedArgs() {
        final Name xName = NameFactory.getName();
        final Name yName = NameFactory.getName();
        final Name zName = NameFactory.getName();

        // Declaration nodes for three local variables.
        AJCVariableDecl xEqThree = f.local(xName, f.Int(), f.l(3));  // int x = 3;
        AJCVariableDecl xDecl = f.local(xName, f.Int());  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl zDecl = f.local(zName, f.Int(), f.plus(f.Ident(xSym), f.Ident(xSym)));

        AJCAssign xAsgSeven = f.Assign(f.Ident(xSym), f.l(7));      // x = 7;

        /*
        int x = 3;
        x = 7;
        int z = x + x;
         */
        AJCMethodDecl loneAssignments = f.MethodFromBlock(f.Block(treeCopier.copy(xEqThree),
                                                              treeCopier.copy(xAsgSeven),
                                                              treeCopier.copy(zDecl)));

        AJCMethodDecl expected = f.MethodFromBlock(f.Block());
        expected.getDecoratedTree().name = loneAssignments.getName();
        expected.getDecoratedTree().sym = loneAssignments.getTargetSymbol();


        /*
        int x = 3;
        x = 7;
        int z = x + x;
        f(x);
         */
        AJCCall callForX = f.callFor(xSym);
        AJCMethodDecl usedAssignments = f.MethodFromBlock(f.Block(treeCopier.copy(xEqThree),
                                                              treeCopier.copy(xAsgSeven),
                                                              treeCopier.copy(zDecl),
                                                              treeCopier.copy(callForX)));

        AJCMethodDecl expected2 = f.MethodFromBlock(f.Block(treeCopier.copy(xDecl),
                treeCopier.copy(xAsgSeven),
                treeCopier.copy(callForX)));

        expected2.getDecoratedTree().name = usedAssignments.getName();
        expected2.getDecoratedTree().sym = usedAssignments.getTargetSymbol();

        /*
        int x = 3;
        switch(x) {
            case 3:
                x = 3;
                break;
            default:
                x = 8;
                break;
        }
         */

        AJCAssign xAsgEight = f.Assign(f.Ident(xSym), f.l(8));      // x = 8;
        AJCAssign xAsgThree = f.Assign(f.Ident(xSym), f.l(3));      // x = 3;

        AJCCase c1 = f.Case(f.l(3), List.<AJCStatement>of(f.Exec(xAsgEight)));
        AJCCase def = f.Case(f.EmptyExpression(), List.<AJCStatement>of(f.Exec(xAsgThree)));
        AJCSwitch aSwitch = f.Switch(f.Ident(xSym), List.of(c1, def));

        AJCMethodDecl switchTest = f.MethodFromBlock(f.Block(treeCopier.copy(xEqThree),
                                                         treeCopier.copy(aSwitch),
                                                         treeCopier.copy(callForX)));

        AJCMethodDecl switchExpected = f.MethodFromBlock(f.Block(treeCopier.copy(xEqThree),
                                                             treeCopier.copy(aSwitch),
                                                             treeCopier.copy(callForX)));

        switchExpected.getDecoratedTree().name = switchTest.getName();
        switchExpected.getDecoratedTree().sym = switchTest.getTargetSymbol();


        /*
        int x = 3;  <--- Redundant
        int y = 3;
        switch(y) {
            case 3:
                x = 3;
                break;
            default:
                x = 8;
                break;
        }
         */

        AJCVariableDecl yEqThree = f.local(yName, f.Int(), f.l(3));  // int y = 3;
        VarSymbol ySym = yEqThree.getTargetSymbol();

        AJCSwitch aSwitch2 = f.Switch(f.Ident(ySym), List.of(treeCopier.copy(c1), treeCopier.copy(def)));

        AJCMethodDecl switchTest2 = f.MethodFromBlock(f.Block(treeCopier.copy(xEqThree),
                                                          treeCopier.copy(yEqThree),
                                                          treeCopier.copy(aSwitch2),
                                                          treeCopier.copy(callForX)));

        AJCMethodDecl switchExpected2 = f.MethodFromBlock(f.Block(treeCopier.copy(xDecl),
                                                              treeCopier.copy(yEqThree),
                                                              treeCopier.copy(aSwitch2),
                                                              treeCopier.copy(callForX)));

        switchExpected2.getDecoratedTree().name = switchTest2.getName();
        switchExpected2.getDecoratedTree().sym = switchTest2.getTargetSymbol();



        /*
        int x = 3;  <--- Redundant
        int y = 3;
        switch(y) {
            case 3:
                x = 3;
                break;
            default:
                x = 8;
                break;
        }
         */

        return
        $(
            $(loneAssignments, expected),
            $(usedAssignments, expected2),
            $(switchTest2, switchExpected2),
            $(switchTest, switchExpected)
        );
    }
}
