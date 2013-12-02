package testinputs;

import testutils.BaseIntegrationTestCase;

public class testAssertionStripper extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int x = 3;
        assert(x == 3);
    }
}
