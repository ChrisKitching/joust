package testinputs;

import testutils.BaseIntegrationTestCase;

/**
 * A test to verify that the semantics of parens have not been obviously ruined.
 */
public class testParens extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int x = 3;
        int y = 7;
        int z = 13;
        int a = 42;
        int r = ((x * x) + (y - (z + a))) * 3;

        print(r);
    }
}
