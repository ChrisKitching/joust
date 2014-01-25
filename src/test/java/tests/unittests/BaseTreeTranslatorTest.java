package tests.unittests;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import joust.optimisers.translators.BaseTranslator;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base class for unit tests to test a type of TreeTranslators.
 *
 * @param <T> The type of TreeTranslator under test.
 */
public @Log4j2
abstract class BaseTreeTranslatorTest<T extends TreeTranslator> extends TreeFabricatingTest {

    // The class of the tree translator type of interest.
    private final Class<T> mClass;
    private Field mResultField;

    public BaseTreeTranslatorTest(Class<T> translatorClass) {
        mClass = translatorClass;
        try {
            mResultField = TreeTranslator.class.getDeclaredField("result");
            mResultField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Get the value of the result field from the given translator.
     */
    protected JCTree getResultFromTranslator(T translator) {
        try {
            return (JCTree) mResultField.get(translator);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        return null;
    }

    /**
     * General method for running a translator function test. Takes the name of the tree translator
     * function to test, the input tree and the expected output tree, and runs the test.
     *
     * @param methodName The name of the TreeTranslator method to test, such as visitUnary.
     * @param input The tree to pass to the translator function under test.
     * @param expected The expected result of applying this translator function to the input tree.
     * @throws Exception In the event of assertion failure.
     */
    private void testVisitNode(String methodName, JCTree input, JCTree expected) throws Exception {
        T translator = mClass.newInstance();

        Method[] methods = mClass.getDeclaredMethods();

        // Find the named method in the type of translator this class is built to test and invoke it.
        boolean foundMethod = false;
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals(methodName)) {
                foundMethod = true;
                m.invoke(translator, input);
                break;
            }
        }

        // Check for insanity situations
        assertTrue(foundMethod);

        log.info("Result: {}  Expected: {}", getResultFromTranslator(translator), expected);

        // Ensure that the actual output matches the expected output.
        // TODO: It may become necessary to implement a proper comparator for JCTree objects.
        assertEquals(expected.toString(), getResultFromTranslator(translator).toString());
    }

    // Dreadful boilerplate functions for each type of tree node..

    public void testVisitTopLevel(JCCompilationUnit input, JCTree expected) throws Exception {
        testVisitNode("visitTopLevel", input, expected);
    }

    public void testVisitImport(JCImport input, JCTree expected) throws Exception {
        testVisitNode("visitImport", input, expected);
    }

    public void testVisitClassDef(JCClassDecl input, JCTree expected) throws Exception {
        testVisitNode("visitClassDef", input, expected);
    }

    public void testVisitMethodDef(JCMethodDecl input, JCTree expected) throws Exception {
        testVisitNode("visitMethodDef", input, expected);
    }

    public void testVisitVarDef(JCVariableDecl input, JCTree expected) throws Exception {
        testVisitNode("visitVarDef", input, expected);
    }

    public void testVisitSkip(JCSkip input, JCTree expected) throws Exception {
        testVisitNode("visitSkip", input, expected);
    }

    public void testVisitBlock(JCBlock input, JCTree expected) throws Exception {
        testVisitNode("visitBlock", input, expected);
    }

    public void testVisitDoLoop(JCDoWhileLoop input, JCTree expected) throws Exception {
        testVisitNode("visitDoLoop", input, expected);
    }

    public void testVisitWhileLoop(JCWhileLoop input, JCTree expected) throws Exception {
        testVisitNode("visitWhileLoop", input, expected);
    }

    public void testVisitForLoop(JCForLoop input, JCTree expected) throws Exception {
        testVisitNode("visitForLoop", input, expected);
    }

    public void testVisitForeachLoop(JCEnhancedForLoop input, JCTree expected) throws Exception {
        testVisitNode("visitForeachLoop", input, expected);
    }

    public void testVisitLabelled(JCLabeledStatement input, JCTree expected) throws Exception {
        testVisitNode("visitLabelled", input, expected);
    }

    public void testVisitCase(JCCase input, JCTree expected) throws Exception {
        testVisitNode("visitCase", input, expected);
    }

    public void testVisitSynchronized(JCSynchronized input, JCTree expected) throws Exception {
        testVisitNode("visitSynchronized", input, expected);
    }

    public void testVisitTry(JCTry input, JCTree expected) throws Exception {
        testVisitNode("visitTry", input, expected);
    }

    public void testVisitCatch(JCCatch input, JCTree expected) throws Exception {
        testVisitNode("visitCatch", input, expected);
    }

    public void testVisitIf(JCIf input, JCTree expected) throws Exception {
        testVisitNode("visitIf", input, expected);
    }

    public void testVisitExec(JCExpressionStatement input, JCTree expected) throws Exception {
        testVisitNode("visitExec", input, expected);
    }

    public void testVisitBreak(JCBreak input, JCTree expected) throws Exception {
        testVisitNode("visitBreak", input, expected);
    }

    public void testVisitContinue(JCContinue input, JCTree expected) throws Exception {
        testVisitNode("visitContinue", input, expected);
    }

    public void testVisitReturn(JCReturn input, JCTree expected) throws Exception {
        testVisitNode("visitReturn", input, expected);
    }

    public void testVisitThrow(JCThrow input, JCTree expected) throws Exception {
        testVisitNode("visitThrow", input, expected);
    }

    public void testVisitApply(JCMethodInvocation input, JCTree expected) throws Exception {
        testVisitNode("visitApply", input, expected);
    }

    public void testVisitNewClass(JCNewClass input, JCTree expected) throws Exception {
        testVisitNode("visitNewClass", input, expected);
    }

    public void testVisitNewArray(JCNewArray input, JCTree expected) throws Exception {
        testVisitNode("visitNewArray", input, expected);
    }

    public void testVisitLambda(JCLambda input, JCTree expected) throws Exception {
        testVisitNode("visitLambda", input, expected);
    }

    public void testVisitParens(JCParens input, JCTree expected) throws Exception {
        testVisitNode("visitParens", input, expected);
    }

    public void testVisitAssign(JCAssign input, JCTree expected) throws Exception {
        testVisitNode("visitAssign", input, expected);
    }

    public void testVisitAssignop(JCAssignOp input, JCTree expected) throws Exception {
        testVisitNode("visitAssignop", input, expected);
    }

    public void testVisitUnary(JCUnary input, JCTree expected) throws Exception {
        testVisitNode("visitUnary", input, expected);
    }

    public void testVisitBinary(JCBinary input, JCTree expected) throws Exception {
        testVisitNode("visitBinary", input, expected);
    }

    public void testVisitTypeCast(JCTypeCast input, JCTree expected) throws Exception {
        testVisitNode("visitTypeCast", input, expected);
    }

    public void testVisitTypeTest(JCInstanceOf input, JCTree expected) throws Exception {
        testVisitNode("visitTypeTest", input, expected);
    }

    public void testVisitIndexed(JCArrayAccess input, JCTree expected) throws Exception {
        testVisitNode("visitIndexed", input, expected);
    }

    public void testVisitSelect(JCFieldAccess input, JCTree expected) throws Exception {
        testVisitNode("visitSelect", input, expected);
    }

    public void testVisitReference(JCMemberReference input, JCTree expected) throws Exception {
        testVisitNode("visitReference", input, expected);
    }

    public void testVisitIdent(JCIdent input, JCTree expected) throws Exception {
        testVisitNode("visitIdent", input, expected);
    }

    public void testVisitLiteral(JCLiteral input, JCTree expected) throws Exception {
        testVisitNode("visitLiteral", input, expected);
    }

    public void testVisitTypeIdent(JCPrimitiveTypeTree input, JCTree expected) throws Exception {
        testVisitNode("visitTypeIdent", input, expected);
    }

    public void testVisitTypeArray(JCArrayTypeTree input, JCTree expected) throws Exception {
        testVisitNode("visitTypeArray", input, expected);
    }

    public void testVisitTypeApply(JCTypeApply input, JCTree expected) throws Exception {
        testVisitNode("visitTypeApply", input, expected);
    }

    public void testVisitTypeUnion(JCTypeUnion input, JCTree expected) throws Exception {
        testVisitNode("visitTypeUnion", input, expected);
    }

    public void testVisitTypeIntersection(JCTypeIntersection input, JCTree expected) throws Exception {
        testVisitNode("visitTypeIntersection", input, expected);
    }

    public void testVisitTypeParameter(JCTypeParameter input, JCTree expected) throws Exception {
        testVisitNode("visitTypeParameter", input, expected);
    }

    public void testVisitWildcard(JCWildcard input, JCTree expected) throws Exception {
        testVisitNode("visitWildcard", input, expected);
    }

    public void testVisitTypeBoundKind(TypeBoundKind input, JCTree expected) throws Exception {
        testVisitNode("visitTypeBoundKind", input, expected);
    }

    public void testVisitAnnotation(JCAnnotation input, JCTree expected) throws Exception {
        testVisitNode("visitAnnotation", input, expected);
    }

    public void testVisitModifiers(JCModifiers input, JCTree expected) throws Exception {
        testVisitNode("visitModifiers", input, expected);
    }

    public void testVisitAnnotatedType(JCAnnotatedType input, JCTree expected) throws Exception {
        testVisitNode("visitAnnotatedType", input, expected);
    }

    public void testVisitErrorneous(JCErroneous input, JCTree expected) throws Exception {
        testVisitNode("visitErroneous", input, expected);
    }

    public void testVisitLetExpr(LetExpr input, JCTree expected) throws Exception {
        testVisitNode("visitLetExpr", input, expected);
    }
}
