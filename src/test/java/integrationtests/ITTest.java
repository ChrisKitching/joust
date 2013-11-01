package integrationtests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ITTest {
    @Test
    public void testFrameWorkExistent() {
        System.out.println("Integration Test framework is alive!");
        assertTrue(true);
    }
}
