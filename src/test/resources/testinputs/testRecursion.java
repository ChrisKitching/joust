package testinputs;

import testutils.BaseIntegrationTestCase;

public class testRecursion extends BaseIntegrationTestCase {
    private int globalA = 5;
    private int globalB = 5;
    private int globalC = 5;


    @Override
    protected void test() {
        int turtles = A(42);

        print(turtles);
    }

    private int A(int x) {
        if (x < 0 ){
            return x;
        }
        globalA++;
        return 1 + B(x - 1);
    }

    private int B(int x) {
        if (x < 0 ){
            return x;
        }
        globalB++;
        return 2 * C(x - 1);
    }

    private int C(int x) {
        if (x < 0 ){
            return x;
        }
        globalC++;
        return A(x - 1) + 42;
    }

    private void D() {

    }

    private void E() {

    }
}
