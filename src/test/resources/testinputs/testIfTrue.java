package testinputs;

import integrationtests.testutils.BaseTest;

/**
 * Test of the dummy optimiser. Not actually possible for this test to ever pass, since the
 * transformation applied is, by design, unsafe.
 *
 * Useful tests will exist shortly.
 */
public class testIfTrue extends BaseTest {
    @Override
    protected void test() {
        if (this.getClass().getName().equals("testIfTrue")) {
            print("SPAAACCEE");
        } else {
            print("CAAAKKE");
        }

        if (false) {
            print("False is true!");
        }

        if (true) {
            print("True is true. Surprisingly.");
        }
    }
}
