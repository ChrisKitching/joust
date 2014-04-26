package testinputs;

import testutils.BaseIntegrationTestCase;

public class testAutoUnbox extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        Integer boxed = 3;
        boxed++;
        boxed--;

        Integer alsoBoxed = 5;
        boxed = boxed + alsoBoxed;

        boolean magic = boxed > alsoBoxed;
        print(magic);
    }
}
