package testinputs;

import testutils.BaseIntegrationTestCase;

public class testFunctionalAvail extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int a = noSideEffectsNoArgs();
        int b = noSideEffectsNoArgs();

        int c = noSideEffectsSomeArgs(a, b);
        int d = noSideEffectsSomeArgs(a, b);
        a++;

        int e = noSideEffectsSomeArgs(a, b);

        print(a);
        print(b);
        print(c);
        print(d);
        print(e);
    }

    private int noSideEffectsNoArgs() {
        return 5 + 3;
    }

    private int noSideEffectsSomeArgs(int a, int b) {
        return a * b;
    }
}
