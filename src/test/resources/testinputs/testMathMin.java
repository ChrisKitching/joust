package testinputs;

import testutils.BaseIntegrationTestCase;

public class testMathMin extends BaseIntegrationTestCase {
    private static final int INITSIZE = 10000;
    private static final int MAXSIZE = 100;
    private static final double TARGETTIME = 10.0;

    @Override
    protected void test() {
        double time = 0.0;
        int size = INITSIZE;
        double d1=1.0D, d2=2.0D, d3=3.0D, d4=4.0D;

        while (time < TARGETTIME && size < MAXSIZE){
            for (int i = 0; i < size; i++){
                d1=Math.min(d2,d3);
                d2=Math.min(d3,d4);
                d3=Math.min(d4,d1);
                d4=Math.min(d1,d2);
                d1=Math.min(d2,d3);
                d2=Math.min(d3,d4);
                d3=Math.min(d4,d1);
                d4=Math.min(d1,d2);
                d1=Math.min(d2,d3);
                d2=Math.min(d3,d4);
                d3=Math.min(d4,d1);
                d4=Math.min(d1,d2);
                d1=Math.min(d2,d3);
                d2=Math.min(d3,d4);
                d3=Math.min(d4,d1);
                d4=Math.min(d1,d2);
            }

            // try to defeat dead code elimination
            if (d1 == -1.0D) {
                print(d1);
            }

            size *=2;
        }

        print(d1);
    }
}
