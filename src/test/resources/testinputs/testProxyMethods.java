package testinputs;

import testutils.BaseIntegrationTestCase;

public class testProxyMethods extends BaseIntegrationTestCase {
    public int publicField = 3;
    protected int protectedField = 2;
    protected int privateField = 1;
    int packageField = 0;

    @Override
    protected void test() {
        doStuff();
    }

    public void doStuff() {
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                System.out.println("Runnable!");
                privateMethod();
                protectedMethod();
                publicMethod();
                packageMethod();

                System.out.println(publicField);
                System.out.println(protectedField);
                System.out.println(privateField);
                System.out.println(packageField);
            }
        };

        runner.run();
    }

    protected void privateMethod() {
        System.out.println("Private.");
    }
    protected void protectedMethod() {
        System.out.println("Protected.");
    }
    public void publicMethod() {
        System.out.println("Public.");
    }
    void packageMethod() {
        System.out.println("Package.");
    }
}
