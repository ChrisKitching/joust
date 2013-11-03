package integrationtests.testutils;

/**
 * Interface specifying the method each test program is to implement. Provides the test harness with
 * a predictable entry point to run the test program after compilation.
 */
public abstract class BaseTest {
    private StringBuilder mTestOutput = new StringBuilder(2048);

    /**
     * Method through which output from the test program is to be routed. The results are stored and
     * collected by the test harness at the end of the test.
     *
     * @param s The line of output the test program wishes to print.
     */
    protected void print(String s) {
        mTestOutput.append(s);
        mTestOutput.append('\n');
    }

    protected void print(Object o) {
        print(o.toString());
    }

    /**
     * The method to be overridden in each test case.
     */
    protected abstract void test();

    /**
     * Run the test and return the complete output as a string.
     *
     * @return The output of the test program.
     */
    public String runTest() {
        test();
        return mTestOutput.toString();
    }
}
