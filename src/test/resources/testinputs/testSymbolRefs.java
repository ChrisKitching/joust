package testinputs;

import testutils.BaseIntegrationTestCase;

public class testSymbolRefs extends BaseIntegrationTestCase {
    public static int cake() {
        return 3;
    }

    public void turtles() {
        print(3);
    }

    public int aField = 42;

    public int a = 12;
    @Override
    protected void test() {
        print(cake());
        print(testSymbolRefs.cake());

        testSymbolRefs instance = new testSymbolRefs();
        print(instance.aField);
        print(aField);

        class cake {
            public int a = 23;

            public void doStuff() {
                print (a + testSymbolRefs.this.a);

            }
        }

        new cake().doStuff();
    }
}
