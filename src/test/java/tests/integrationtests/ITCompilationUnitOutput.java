package tests.integrationtests;

import testutils.BaseIntegrationTestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Parameterised JUnit test to ensure the output of the test programs in the resources directory
 * are unchanged after optimisation.
 * The parameter function gets the list of all input files from the resource directory, causing
 * JUnit to produce an instance of the test for each such file, tidily giving us one test per
 * compilation unit without the need for lots of boring boilerplate.
 */
@RunWith(Parameterized.class)
public class ITCompilationUnitOutput {
    private static Logger logger = LogManager.getLogger();

    public static final String TEST_INPUTS_DIR = "/testinputs/";
    public static final String OPT_DIR = "/compilationResults/opt/";
    public static final String UN_OPT_DIR = "/compilationResults/noOpt/";

    public static final String TEST_INPUT_PACKAGE = "testinputs";

    private static final URL sOptOutDir = ITCompilationUnitOutput.class.getResource(OPT_DIR);
    private static final URL sUnOptOutDir = ITCompilationUnitOutput.class.getResource(UN_OPT_DIR);

    private static final String JAVAP = System.getProperty("java.home")+"/../bin/javap";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        URL testInputsUrl = ITCompilationUnitOutput.class.getResource(TEST_INPUTS_DIR);

        File optOutDir = new File(sOptOutDir.getFile());
        File noOptOutdir = new File(sUnOptOutDir.getFile());

        File testInputs = new File(testInputsUrl.getFile());
        File[] testCases = testInputs.listFiles();
        System.out.println(Arrays.toString(testCases));

        LinkedList<Object[]> ret = new LinkedList<>();
        for (int i = 0; i < testCases.length; i++) {
            ret.add(new Object[] {testCases[i], optOutDir, noOptOutdir});
        }

        return ret;
    }

    // A pair of classloaders - one for optimised classes, one for unoptimised classes.
    public static URLClassLoader sOptClassLoader =  new URLClassLoader(new URL[] {sOptOutDir});
    public static URLClassLoader sUnOptClassLoader =  new URLClassLoader(new URL[] {sUnOptOutDir});

    private File mTargetSource;
    private File mOptOutDir;
    private File mNoOptOutDir;

    private String mTestClassName;
    private String mFullyQualifiedTestClassName;

    public ITCompilationUnitOutput(File elementName, File optOutDir, File noOptOutDir) {
        mTargetSource = elementName;
        mOptOutDir = optOutDir;
        mNoOptOutDir = noOptOutDir;

        String n = elementName.getName();
        mTestClassName = n.substring(0, n.lastIndexOf('.'));
        mFullyQualifiedTestClassName = TEST_INPUT_PACKAGE + '.' + mTestClassName;
    }

    /**
     * Compile the given .java with and without optimisation, run both, and ensure the outputs
     * are the same.
     */
    @Test
    public void runTest() {
        logger.info(mTargetSource);

        // Create the compiled programs..
        assertTrue(compileTarget(false));
        assertTrue(compileTarget(true));

        // Execute them both and collect their output...
        Class<? extends BaseIntegrationTestCase> optClass = null;
        Class<? extends BaseIntegrationTestCase> noOptClass = null;
        try {
            optClass = (Class<? extends BaseIntegrationTestCase>) sOptClassLoader.loadClass(mFullyQualifiedTestClassName);
            noOptClass = (Class<? extends BaseIntegrationTestCase>) sUnOptClassLoader.loadClass(mFullyQualifiedTestClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Verify we loaded the classes successfully...
        assertNotNull(optClass);
        assertNotNull(noOptClass);

        String optOutput = executeCompiledTest(optClass);
        logger.debug("Optimised output:\n{}", optOutput);
        String noOptOutput = executeCompiledTest(noOptClass);
        logger.debug("Unoptimised output:\n{}", noOptOutput);

        // Not using an assertion directly so we can print extra debug information.
        if (!optOutput.equals(noOptOutput)) {
            // Print a bytecode diff for debugging...
            if (logger.isDebugEnabled()) {
                printBytecodeOfTest();
            }

            // ...And fail the test.
            assertTrue(false);
        }
    }

    /**
     * Print the bytecode of the failing programs, to aid in debugging.
     */
    private void printBytecodeOfTest() {
        try {
            logger.debug("-------------- BYTECODE OF OPTIMISED TESTCASE --------------");
            printBytecodeForDir(sOptOutDir);
            logger.debug("-------------- BYTECODE OF UNOPTIMISED TESTCASE --------------");
            printBytecodeForDir(sUnOptOutDir);
        } catch (IOException e) {
            logger.debug("IOException dumping bytecode!");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to print the bytecode of the test class for this test at the directory given.
     * Typically called once for optimised builds, and again for the unoptimised build.
     *
     * @param dirName Top-level output directory to look in for the target class.
     * @throws IOException If an IOException is thrown by ProcessBuilder.start trying to call javap.
     */
    private void printBytecodeForDir(URL dirName) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] cmd = new String[]{JAVAP, "-v", "-classpath", ".", mFullyQualifiedTestClassName};

        Process proc = rt.exec(cmd, null, new File(dirName.getFile()));

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        String s;

        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
    }

    /**
     * Given a class that extends BaseIntegrationTestCase, run the test and collect the output.
     *
     * @param targetClass
     * @return The output of the test referred to by this class.
     */
    private String executeCompiledTest(Class<? extends BaseIntegrationTestCase> targetClass) {
        BaseIntegrationTestCase t = null;

        try {
            t = targetClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        assertNotNull(t);

        return t.runTest();
    }

    /**
     * Helper method to compile the target for this test with or without optimisation.
     *
     * @param optimise If true, apply the optimiser. Otherwise, do not apply the optimiser.
     */
    private boolean compileTarget(boolean optimise) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationTarget = fileManager.getJavaFileObjects(mTargetSource);

        List<String> optionList = new ArrayList<>();
        if (!optimise) {
            optionList.add("-proc:none");
        }
        optionList.add("-d");
        if (optimise) {
            optionList.add(mOptOutDir.getPath());
        } else {
            optionList.add(mNoOptOutDir.getPath());
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, optionList, null, compilationTarget);

        // Perform the compilation task.
        return task.call();
    }
}
