package testinputs;

import testutils.BaseIntegrationTestCase;

public class testExpressionNormalisation extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int x = 5 + 3;
        int y = 5;
        int z = 3;

        int a = z + y + x + 5;
        int b = y + z + 5 + x;
        int c = x + z + 5 + y;

        print(a);
        print(b);
        print(c);


        a = z + (y / 5) + x + (5 * x * (z + 3));
        b = y + z + 5 + x;
        c = x + z + 5 + y;
    }
}
