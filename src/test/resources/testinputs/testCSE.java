package testinputs;

import testutils.BaseIntegrationTestCase;

public class testCSE extends BaseIntegrationTestCase {
    public int someGlobal = 0;

    @Override
    protected void test() {
        int a = 1;
        int b = 2;
        int c = 4;
        int d = 5;
        int e = 6;
        int f = 7;

        int x = (a + b) + fac(c);
        int y = (a + b) + d;

        print(f);
        print(f);
        print(f);
        print(f);

        int z = (a + b) + fac(c);
        int t = (b + a) + d;

        int v = (a * b) + fac(c);
        int u = (b * a) + fac(c);

        f += x + y + z + t + v + u;

        print(a + ", " + b + ", " + c + ", " + d + ", " + e + ", " + f);
    }

    public int fac(int x) {
        if (x == 0) {
            return 1;
        }

        return x * fac(x - 1);
    }
}
