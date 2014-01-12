package testinputs;

import testutils.BaseIntegrationTestCase;

public class testScoping extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int cake = 2;
        String turtles = "Turtles.";
        int answer = 0;
        while (answer != 42) {
            String moreTurtles = "Did I mention: Turtles.";
            int pointlessIntermediate = answer + cake;
            answer = pointlessIntermediate;
        }
        print (answer);
        print (turtles);
    }
}
