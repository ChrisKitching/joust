package testinputs;

import testutils.BaseIntegrationTestCase;

public class testRedundantAssignments extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int a = 5; // Redundant.
        a = 10;
        a++;

        print(a);

        // Check for redundancy across branches...
        int b = 3;  // Redundant.
        int c = 12; // Not redundant.
        if (a > 5) {
            b = 5;
            c = 6;
            if (c == 6) {
                b = 3;
            } else {
                b = 6;
            }
        } else {
            b = 7;
        }

        print(b + ", " + c);

        int d = 12;
        int e = 5;
        switch (c) {
            case 10:
                d = 6;
            break;
            case 12:
                d = 7;
            break;
            case 1:
                d = 8;
                e = 4;
            break;
            default:
                d = 3;
                break;
        }


        print(d + ", " + e);
    }
}
