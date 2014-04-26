package tests.unittests;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.util.List;
import joust.tree.annotatedtree.AJCForest;
import joust.tree.annotatedtree.AJCTree;
import joust.tree.annotatedtree.AJCTreeVisitor;
import joust.tree.annotatedtree.treeinfo.TreeInfoManager;
import joust.utils.logging.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Logger;
import static org.junit.Assert.*;

/**
 * Base class for testing tree visitors.
 */
@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
public abstract class BaseTreeVisitorTest<T extends AJCTreeVisitor> extends TreeFabricatingTest {
    // The class of the tree translator type of interest.
    protected T visitorInstance;

    protected Class<T> visitorClass;

    public BaseTreeVisitorTest(Class<T> vClass, Object... constructorArgs) {
        visitorClass = vClass;
        createVisitorInstance(constructorArgs);
    }

    /**
     * Create a new instance of the visitor class.
     */
    protected void createVisitorInstance(Object... constructorArgs) {
        Class<?>[] argTypes = new Class<?>[constructorArgs.length];
        for (int i = 0; i < constructorArgs.length; i++) {
            argTypes[i] = constructorArgs[i].getClass();
        }

        try {
            visitorInstance = visitorClass.getConstructor(argTypes).newInstance(constructorArgs);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Error instantiating visitor.", e);
            fail();
        }
    }

    /**
     * Apply the visitor instance to the input tree.
     * @param input Tree to visit.
     */
    protected void doVisit(AJCTree input) {
        visitorInstance.visitTree(input);
    }

    /**
     * Shorthand function for creating sets of symbols for test parameters.
     */
    protected static Symbol.VarSymbol[] $v(Symbol.VarSymbol... vSyms) {
        return vSyms;
    }

    /**
     * Mock out the AJCForest instance. Tests that require this should call it before each run.
     */
    protected void prepareTrees(AJCTree input, AJCTree expected) {
        AJCForest.uninit();
        TreeInfoManager.init();

        HashMap<Symbol.MethodSymbol, AJCTree.AJCMethodDecl> methodTable = new HashMap<>();
        if (input instanceof AJCTree.AJCMethodDecl) {
            AJCTree.AJCMethodDecl cast = (AJCTree.AJCMethodDecl) input;
            methodTable.put(cast.getTargetSymbol(), cast);
        }

        AJCForest.initDirect(List.of(input), methodTable, new HashMap<AJCTree, Env<AttrContext>>());

        AJCForest.getInstance().effectVisitor.visitTree(expected);
    }
}
