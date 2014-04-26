package testinputs;

import testutils.BaseIntegrationTestCase;

import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test of the dummy optimiser. Not actually possible for this test to ever pass, since the
 * transformation applied is, by design, unsafe.
 *
 * Useful tests will exist shortly.
 */
public class testIfTrue extends BaseIntegrationTestCase {
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

        forUnboundMulticastSocket();
    }

    public static class A {
        private final List<Integer> l;
        private final List<InterfaceAddress> p;

        public A(List<Integer> l, List<InterfaceAddress> p) {
            this.l = l;
            this.p = p;
        }
    }

    static A forUnboundMulticastSocket() {
        // This is what the RI returns for a MulticastSocket that hasn't been constrained
        // to a specific interface.
        return new A(Arrays.asList(3), Collections.<InterfaceAddress>emptyList());
    }
}
