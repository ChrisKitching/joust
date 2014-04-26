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
import static com.sun.tools.javac.code.Symbol.*;

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

        AJCVariableDecl aDecl = f.local(aName, f.Int(), f.l(1));
        AJCVariableDecl bDecl = f.local(bName, f.Int(), f.l(2));
        AJCVariableDecl cDecl = f.local(cName, f.Int(), f.l(3));
        AJCVariableDecl dDecl = f.local(dName, f.Int(), f.l(4));
        AJCVariableDecl eDecl = f.local(eName, f.Int(), f.l(5));
        AJCVariableDecl fDecl = f.local(fName, f.Int(), f.l(6));

        VarSymbol aSym = aDecl.getTargetSymbol();
        VarSymbol bSym = bDecl.getTargetSymbol();
        VarSymbol cSym = cDecl.getTargetSymbol();
        VarSymbol dSym = dDecl.getTargetSymbol();
        VarSymbol eSym = eDecl.getTargetSymbol();
        VarSymbol fSym = fDecl.getTargetSymbol();

        // Idents for the temporary variables...
        AJCIdent aIdent = f.Ident(aSym);
        AJCIdent bIdent = f.Ident(bSym);
        AJCIdent cIdent = f.Ident(cSym);
        AJCIdent dIdent = f.Ident(dSym);
        AJCIdent eIdent = f.Ident(eSym);
        AJCIdent fIdent = f.Ident(fSym);

        /*
        while (a < e) {
            d = b + c;
            f = b + c;
            a++;
        }
         */

        AJCBinary aLTe = f.lt($t(aIdent), $t(eIdent));  // a < e
        AJCBinary bPlusc = f.plus($t(bIdent), $t(cIdent));  // b + c
        AJCBinary bPlusc2 = $t(bPlusc);  // b + c

        AJCAssign dAsgBPlusc = f.Assign($t(dIdent), bPlusc);  // d = b + c
        AJCAssign fAsgBPlusc = f.Assign($t(fIdent), bPlusc2);  // f = b + c
        AJCUnaryAsg aPlusPlus = f.postInc(aIdent);   // a++

        AJCWhileLoop whileAE = f.WhileLoop(aLTe, f.Block(dAsgBPlusc, fAsgBPlusc, aPlusPlus));

        /*
        for (int i = 0; i < 10; i++) {
            d = (b + c * a) + i;
            f = i + (b + c * a);
        }
         */

        AJCVariableDecl iDecl = f.local(NameFactory.getName(), f.Int(), f.l(0));  // int i = 0
        AJCIdent iIdent = f.Ident(iDecl.getTargetSymbol());
        AJCBinary iLT10 = f.lt($t(iIdent), f.l(10));
        AJCExpressionStatement iPlusPlus = f.Exec(f.postInc($t(iIdent)));

        AJCBinary bPlusC3 = $t(bPlusc);
        AJCBinary bPlusC4 = $t(bPlusc);

        AJCBinary bPluscTimesa = f.mul(bPlusC3, $t(aIdent)); // b + c * a;
        AJCBinary bPluscTimesa2 = f.mul(bPlusC4, $t(aIdent)); // b + c * a;

        AJCForLoop forBCAI = f.ForLoop(List.<AJCStatement>of(iDecl), iLT10, List.of(iPlusPlus), f.Block(bPluscTimesa, bPluscTimesa2));

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

        AJCVariableDecl intsDecl = f.local(NameFactory.getName(), f.Array(f.Int()), f.NewArray(f.Array(f.Int()), List.<AJCExpressionTree>of(f.l(3)), List.<AJCExpressionTree>nil()));
        VarSymbol intsSym = intsDecl.getTargetSymbol();
        AJCIdent intsIdent = f.Ident(intsSym);

        AJCBinary iLT3 = f.lt($t(iIdent), f.l(3));
        AJCVariableDecl iDeclOne = $t(iDecl);  // int i = 1;
        iDeclOne.setInit(f.l(1));

        AJCArrayAccess intsI = f.ArrayAccess($t(intsIdent), $t(iIdent));
        AJCArrayAccess intsImm = f.ArrayAccess($t(intsIdent), f.minus($t(iIdent), f.l(1)));

        AJCArrayAccess ints1 = f.ArrayAccess($t(intsIdent), f.l(1));
        AJCArrayAccess ints2 = f.ArrayAccess($t(intsIdent), f.l(2));
        AJCBinary intsSum = f.plus(ints1, ints2);
        AJCBinary intsRelSum = f.plus(intsImm, $t(intsI));


        AJCBlock bodyOne = f.Block(f.Assign($t(intsI), $t(iIdent)));
        AJCBlock bodyTwo = f.Block(f.Assign($t(intsI), intsSum));
        AJCBlock bodyThree = f.Block(f.Assign($t(intsI), intsRelSum));
        AJCBlock bodyFour = f.Block(f.Assignop(JCTree.Tag.PLUS_ASG, $t(fIdent), $t(intsI)));

        AJCForLoop forOne = f.ForLoop(List.<AJCStatement>of($t(iDecl)), $t(iLT3), List.of($t(iPlusPlus)), bodyOne);
        AJCForLoop forTwo = f.ForLoop(List.<AJCStatement>of($t(iDecl)), $t(iLT3), List.of($t(iPlusPlus)), bodyTwo);
        AJCForLoop forThree = f.ForLoop(List.<AJCStatement>of($t(iDeclOne)), $t(iLT3), List.of($t(iPlusPlus)), bodyThree);
        AJCForLoop forFour = f.ForLoop(List.<AJCStatement>of($t(iDeclOne)), $t(iLT3), List.of($t(iPlusPlus)), bodyFour);


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
        Set<AJCExpressionTree> ts = new HashSet<AJCExpressionTree>();
        for (int i = 0; i < trees.length; i++) {
            ts.add(trees[i]);
        }

        return ts;
    }
}
