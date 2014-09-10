package testinputs;

import testutils.BaseIntegrationTestCase;

public class testIllegalOverride extends BaseIntegrationTestCase {
    public class A {
        void f() {
            System.out.println("Cake");
        }
    }

    public class B extends A {
        void f() {
            System.out.println("Cake");
        }
    }

    private void process(A anA) {
        anA.f();
    }

    @Override
    protected void test() {
        process(new A());
        process(new B());
    }
}
