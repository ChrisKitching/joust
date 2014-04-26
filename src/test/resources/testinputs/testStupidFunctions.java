package testinputs;

import testutils.BaseIntegrationTestCase;

import java.util.ArrayList;

public class testStupidFunctions extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int x = Math.min(1, 3);
        int y = Math.min(Math.min(x, 6), (int) Math.max(5L, 9L));
        long z = Math.abs(y - x);

        switch(y) {
            case 6:
                int p = Math.min(x < y ? x : y, y);
                print(p);
                break;
            case 5:
                print("Toasters.");
                break;
        }

        print(x + ", " + y + ", " + z);
        print("Hello, world".toString());
    }
}
