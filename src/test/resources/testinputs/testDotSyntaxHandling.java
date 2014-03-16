package testinputs;

import testutils.BaseIntegrationTestCase;

/**
 * A horridly contrived testcase to exercise function calls via field references.
 */
public class testDotSyntaxHandling extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        testDotSyntaxHandling.A a = new A();
        testDotSyntaxHandling.B b = new B();
        testDotSyntaxHandling.C c = new C();
        a.b = b;
        b.c = c;

        a.f();
        a.b.f();
        a.b.c.f();
    }

    class A {
        B b;
        void f() {
            print ("A!");
        }
    }

    class B {
        C c;
        void f() {
            print ("B!");
        }
    }

    class C {
        void f() {
            print ("C!");
        }
    }
}
