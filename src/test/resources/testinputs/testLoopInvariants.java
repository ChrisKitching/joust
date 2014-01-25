package testinputs;

import testutils.BaseIntegrationTestCase;

public class testLoopInvariants extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int a = 1;
        int b = 2;
        int c = 4;
        int d = 5;
        int e = 6;
        int f = 7;

        while (a < e) {
            d = b + c;
            f = b + c;
            a++;
        }

        for (int i = 0; i < 10; i++) {
            d = (b + c * a) + i;
            f = i + (b + c * a);
        }

        do {
            a--;
            b = d / e;
            for (int t = 0; t < 10; t++) {
                e += b;
                e += b + c * 12;
            }
        } while (a > 0);

        print (a+", "+b+", "+c+", "+d+", "+e+", "+f);
    }
}
