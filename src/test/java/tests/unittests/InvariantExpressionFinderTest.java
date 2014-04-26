package tests.unittests;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import joust.utils.tree.NameFactory;
import joust.optimisers.invar.InvariantExpressionFinder;
import joust.analysers.sideeffects.SideEffectVisitor;
import joust.tree.annotatedtree.treeinfo.EffectSet;
import joust.utils.logging.LogUtils;
import joust.utils.data.SymbolSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static joust.tree.annotatedtree.AJCTree.*;
import static junitparams.JUnitParamsRunner.$;
import static tests.unittests.utils.ShorthandExpressionFactory.*;
import static com.sun.tools.javac.code.Symbol.*;
import static tests.unittests.utils.UnitTestTreeFactory.*;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnitParamsRunner.class)
public class InvariantExpressionFinderTest extends BaseAnalyserTest<InvariantExpressionFinder> {
    public InvariantExpressionFinderTest() {
        super(InvariantExpressionFinder.class, "invariantExpressions", new SymbolSet(), new SymbolSet());
    }

    @Test
    @Parameters(method = "invarFinderArgs")
    public void findInvariants(AJCEffectAnnotatedTree inputTree, Set<AJCExpressionTree> expectedInvariants) {
        SideEffectVisitor effects = new SideEffectVisitor(true);
        effects.visitTree(inputTree);

        EffectSet loopEffects = inputTree.effects.getEffectSet();

        Set<VarSymbol> writtenInLoop = loopEffects.writeInternal;
        Set<VarSymbol> readInLoop = loopEffects.readInternal;

        log.debug("Invariant seek for:\n", inputTree);
        log.debug("With written: {}", Arrays.toString(writtenInLoop.toArray()));
        log.debug("With read: {}", Arrays.toString(readInLoop.toArray()));

        testVisitNodeStatefully(inputTree, expectedInvariants, writtenInLoop, readInLoop);
    }

    @SuppressWarnings("unchecked")
    public Object[] invarFinderArgs() {
        /*
        int a = 1;
        int b = 2;
        int c = 4;
        int d = 5;
        int e = 6;
        int f = 7;


        while (a < e) {
            d = b + c;
            f = b + c;
            a++;
        }

        for (int i = 0; i < 10; i++) {
            d = (b + c * a) + i;
            f = i + (b + c * a);
        }

        do {
            a--;
            b = d / e;
            for (int t = 0; t < 10; t++) {
                e += b;
                e += b + c * 12;
            }
        } while (a > 0);

        int[] ints = new int[3];
        for (int i = 0; i < 3; i++) {
            ints[i] = i;
        }

        for (int i = 1; i < 3; i++) {
            ints[i] = ints[i-1] + ints[i];
        }

        for (int i = 0; i < 3; i++) {
            ints[i] = ints[1] + ints[2];
        }
         */

        // Some temporary variables...
        Name aName = NameFactory.getName();
        Name bName = NameFactory.getName();
        Name cName = NameFactory.getName();
        Name dName = NameFactory.getName();
        Name eName = NameFactory.getName();
        Name fName = NameFactory.getName();

        AJCVariableDecl aDecl = local(aName, Int(), l(1));
        AJCVariableDecl bDecl = local(bName, Int(), l(2));
        AJCVariableDecl cDecl = local(cName, Int(), l(3));
        AJCVariableDecl dDecl = local(dName, Int(), l(4));
        AJCVariableDecl eDecl = local(eName, Int(), l(5));
        AJCVariableDecl fDecl = local(fName, Int(), l(6));

        VarSymbol aSym = aDecl.getTargetSymbol();
        VarSymbol bSym = bDecl.getTargetSymbol();
        VarSymbol cSym = cDecl.getTargetSymbol();
        VarSymbol dSym = dDecl.getTargetSymbol();
        VarSymbol eSym = eDecl.getTargetSymbol();
        VarSymbol fSym = fDecl.getTargetSymbol();

        // Idents for the temporary variables...
        AJCIdent aIdent = Ident(aSym);
        AJCIdent bIdent = Ident(bSym);
        AJCIdent cIdent = Ident(cSym);
        AJCIdent dIdent = Ident(dSym);
        AJCIdent eIdent = Ident(eSym);
        AJCIdent fIdent = Ident(fSym);

        /*
        while (a < e) {
            d = b + c;
            f = b + c;
            a++;
        }
         */

        AJCBinary aLTe = lt($t(aIdent), $t(eIdent));  // a < e
        AJCBinary bPlusc = plus($t(bIdent), $t(cIdent));  // b + c
        AJCBinary bPlusc2 = $t(bPlusc);  // b + c

        AJCAssign dAsgBPlusc = Assign($t(dIdent), bPlusc);  // d = b + c
        AJCAssign fAsgBPlusc = Assign($t(fIdent), bPlusc2);  // f = b + c
        AJCUnaryAsg aPlusPlus = postInc(aIdent);   // a++

        AJCWhileLoop whileAE = WhileLoop(aLTe, Block(dAsgBPlusc, fAsgBPlusc, aPlusPlus));

        /*
        for (int i = 0; i < 10; i++) {
            d = (b + c * a) + i;
            f = i + (b + c * a);
        }
         */

        AJCVariableDecl iDecl = local(NameFactory.getName(), Int(), l(0));  // int i = 0
        AJCIdent iIdent = Ident(iDecl.getTargetSymbol());
        AJCBinary iLT10 = lt($t(iIdent), l(10));
        AJCExpressionStatement iPlusPlus = Exec(postInc($t(iIdent)));

        AJCBinary bPlusC3 = $t(bPlusc);
        AJCBinary bPlusC4 = $t(bPlusc);

        AJCBinary bPluscTimesa = mul(bPlusC3, $t(aIdent)); // b + c * a;
        AJCBinary bPluscTimesa2 = mul(bPlusC4, $t(aIdent)); // b + c * a;

        AJCForLoop forBCAI = ForLoop(List.<AJCStatement>of(iDecl), iLT10, List.of(iPlusPlus), Block(bPluscTimesa, bPluscTimesa2));

        /*
        int[] ints = new int[3];
        for (int i = 0; i < 3; i++) {
            ints[i] = i;
        }

        for (int i = 0; i < 3; i++) {
            ints[i] = ints[1] + ints[2];
        }

        for (int i = 1; i < 3; i++) {
            ints[i] = ints[i-1] + ints[i];
        }

        for (int i = 1; i < 3; i++) {
            f += ints[i];
        }
         */

        AJCVariableDecl intsDecl = local(NameFactory.getName(), Array(Int()), NewArray(Array(Int()), List.<AJCExpressionTree>of(l(3)), List.<AJCExpressionTree>nil()));
        VarSymbol intsSym = intsDecl.getTargetSymbol();
        AJCIdent intsIdent = Ident(intsSym);

        AJCBinary iLT3 = lt($t(iIdent), l(3));
        AJCVariableDecl iDeclOne = $t(iDecl);  // int i = 1;
        iDeclOne.setInit(l(1));

        AJCArrayAccess intsI = ArrayAccess($t(intsIdent), $t(iIdent));
        AJCArrayAccess intsImm = ArrayAccess($t(intsIdent), minus($t(iIdent), l(1)));

        AJCArrayAccess ints1 = ArrayAccess($t(intsIdent), l(1));
        AJCArrayAccess ints2 = ArrayAccess($t(intsIdent), l(2));
        AJCBinary intsSum = plus(ints1, ints2);
        AJCBinary intsRelSum = plus(intsImm, $t(intsI));


        AJCBlock bodyOne = Block(Assign($t(intsI), $t(iIdent)));
        AJCBlock bodyTwo = Block(Assign($t(intsI), intsSum));
        AJCBlock bodyThree = Block(Assign($t(intsI), intsRelSum));
        AJCBlock bodyFour = Block(Assignop(JCTree.Tag.PLUS_ASG, $t(fIdent), $t(intsI)));

        AJCForLoop forOne = ForLoop(List.<AJCStatement>of($t(iDecl)), $t(iLT3), List.of($t(iPlusPlus)), bodyOne);
        AJCForLoop forTwo = ForLoop(List.<AJCStatement>of($t(iDecl)), $t(iLT3), List.of($t(iPlusPlus)), bodyTwo);
        AJCForLoop forThree = ForLoop(List.<AJCStatement>of($t(iDeclOne)), $t(iLT3), List.of($t(iPlusPlus)), bodyThree);
        AJCForLoop forFour = ForLoop(List.<AJCStatement>of($t(iDeclOne)), $t(iLT3), List.of($t(iPlusPlus)), bodyFour);


        return $(
            $(
                whileAE,
                $s(bPlusc, bPlusc2)
            ),
            $(
                forBCAI,
                $s(bPluscTimesa, bPluscTimesa2, bPlusC3, bPlusC4)
            ),
            $(
                forOne,
                $s()
            ),
            $(
                forTwo,
                $s()
                // TODO: Someday... $s(ints1, ints2, intsSum)
            ),
            $(
                forThree,
                $s()
            ),
            $(
                forFour,
                $s()
            )
        );
    }

    public static Set<AJCExpressionTree> $s(AJCExpressionTree... trees) {
        Set<AJCExpressionTree> ts = new HashSet<>();
        for (int i = 0; i < trees.length; i++) {
            ts.add(trees[i]);
        }

        return ts;
    }
}
