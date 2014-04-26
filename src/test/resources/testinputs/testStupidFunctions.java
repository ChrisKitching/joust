package testinputs;

import testutils.BaseIntegrationTestCase;

public class testStupidFunctions extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int x = Math.min(1, 3);
        int y = Math.min(Math.min(x, 6), (int) Math.max(5L, 9L));
        long z = Math.abs(y - x);

        print(x + ", " + y + ", " + z);
    }
}
