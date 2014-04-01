package testinputs;

import testutils.BaseIntegrationTestCase;

public class testCSE extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int a = 1;
        int b = 2;
        int c = 4;
        int d = 5;
        int e = 6;
        int f = 7;

        int x = (a + b) + c;
        int y = (a + b) + d;

        print(f);
        print(f);
        print(f);
        print(f);

        int z = (a + b) + c;
        int t = (b + a) + d;

        int v = (a * b) + c;
        int u = (b * a) + c;

        f += x + y + z + t + v + u;

        print(a + ", " + b + ", " + c + ", " + d + ", " + e + ", " + f);
    }
}
