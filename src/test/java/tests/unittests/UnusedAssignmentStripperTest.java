package tests.unittests;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.optimisers.avail.NameFactory;
import joust.optimisers.translators.UnusedAssignmentStripper;
import joust.optimisers.visitors.sideeffects.SideEffectVisitor;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static  com.sun.tools.javac.code.Symbol.*;
import static joust.utils.StaticCompilerUtils.treeCopier;
import static tests.unittests.utils.UnitTestTreeFactory.*;

/**
 * Some unit tests for the unused assignment stripper.
 */
@Log4j2
@RunWith(JUnitParamsRunner.class)
public class UnusedAssignmentStripperTest extends BaseTreeTranslatorTest<UnusedAssignmentStripper> {
    public UnusedAssignmentStripperTest() {
        super(UnusedAssignmentStripper.class);
    }

    @Test
    @Parameters(method = "unusedArgs")
    public void testUnused(AJCMethodDecl input, AJCMethodDecl expected) {
        SideEffectVisitor effects = new SideEffectVisitor();
        effects.visitTree(input);

        testVisitNodeBluntForce(input, expected);
    }

    @SuppressWarnings("unchecked")
    public Object[] unusedArgs() {
        final Name xName = NameFactory.getName();
        final Name zName = NameFactory.getName();

        // Declaration nodes for three local variables.
        AJCVariableDecl xEqThree = local(xName, Int(), l(3));  // int x = 3;
        AJCVariableDecl xDecl = local(xName, Int());  // int x = 3;
        VarSymbol xSym = xEqThree.getTargetSymbol();

        AJCVariableDecl zDecl = local(zName, Int(), plus(Ident(xSym), Ident(xSym)));

        AJCAssign xAsgSeven = Assign(Ident(xSym), l(7));      // x = 7;

        /*
        int x = 3;
        x = 7;
        int z = x + x;
         */
        AJCMethodDecl loneAssignments = MethodFromBlock(Block(treeCopier.copy(xEqThree),
                                                              treeCopier.copy(xAsgSeven),
                                                              treeCopier.copy(zDecl)));

        AJCMethodDecl expected = MethodFromBlock(Block());
        expected.getDecoratedTree().name = loneAssignments.getName();
        expected.getDecoratedTree().sym = loneAssignments.getTargetSymbol();


        /*
        int x = 3;
        x = 7;
        int z = x + x;
        f(x);
         */
        AJCCall callForX = callFor(xSym);
        AJCMethodDecl usedAssignments = MethodFromBlock(Block(treeCopier.copy(xEqThree),
                                                              treeCopier.copy(xAsgSeven),
                                                              treeCopier.copy(zDecl),
                                                              treeCopier.copy(callForX)));

        AJCMethodDecl expected2 = MethodFromBlock(Block(treeCopier.copy(xDecl),
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

        AJCAssign xAsgEight = Assign(Ident(xSym), l(8));      // x = 8;
        AJCAssign xAsgThree = Assign(Ident(xSym), l(3));      // x = 3;

        AJCCase c1 = Case(l(3), List.<AJCStatement>of(Exec(xAsgEight)));
        AJCCase def = Case(EmptyExpression(), List.<AJCStatement>of(Exec(xAsgThree)));
        AJCSwitch aSwitch = Switch(Ident(xSym), List.of(c1, def));

        AJCMethodDecl switchTest = MethodFromBlock(Block(treeCopier.copy(xEqThree),
                                                         treeCopier.copy(aSwitch),
                                                         treeCopier.copy(callForX)));

        AJCMethodDecl switchExpected = MethodFromBlock(Block(treeCopier.copy(xDecl),
                                                             treeCopier.copy(aSwitch),
                                                             treeCopier.copy(callForX)));

        switchExpected.getDecoratedTree().name = switchTest.getName();
        switchExpected.getDecoratedTree().sym = switchTest.getTargetSymbol();

        return
        $(
            $(loneAssignments, expected),
            $(usedAssignments, expected2),
            $(switchTest, switchExpected)
        );
    }
}
