package testinputs;

import testutils.BaseIntegrationTestCase;

public class testParameters  extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        print(methodWithParameters(3, 5, 7));
    }

    public int methodWithParameters(int x, int y, int z) {
        x++;
        int a = x + y;
        int b = y + z;
        int c = x + y;

        x++;
        int d = x + y;

        return a + b + c + d;
    }
}
