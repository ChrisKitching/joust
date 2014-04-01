package testinputs;

import testutils.BaseIntegrationTestCase;

public class testArrayInvariants extends BaseIntegrationTestCase {
    @Override
    protected void test() {
        int[] respXORKey = new int[]{1, 2, 3};
        int[] ciphertext = new int[]{1, 2, 3};

        int[] array = new int[3];

        // Lifted from a problematic part of Mozilla's codebase...
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) (respXORKey[i] ^ ciphertext[i]);
        }

        print(array[0] + array[1] + array[2]);
    }
}
