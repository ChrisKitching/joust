package testinputs;

import testutils.BaseIntegrationTestCase;

public class testWhileLoopInsanity extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int a = 0;

        // A single-statement-body while loop...
        while (a < 10) {a++;}
        while (a < 10) {;}

        while (a < 20) {
            a++;
            a++;
        }

        print(a);
    }
}
