package testinputs;

import testutils.BaseIntegrationTestCase;

public class testFinalFold extends BaseIntegrationTestCase {
    public static final int TOAST = 4;
    public static final String TOASTER_TIME = "IT'S TOASTER TIME";
    public static final double halfPi = Math.PI/2;
    public static final double halfPiEasily = 3.14159265358979D/2;
    public static final String CAN_YOU_CONCAT = "IT'S" + " TOASTER " + "TIME (" + 12 + ")";
    public static int cake = 3;

    @Override
    protected void test() {
        double a = TOAST + halfPiEasily;
        double b = halfPi + 12;
        String cake = CAN_YOU_CONCAT + TOASTER_TIME;
        print(a + b);
        print(cake);
    }
}
