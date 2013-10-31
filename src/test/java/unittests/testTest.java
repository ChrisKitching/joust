package unittests;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class testTest {
    @Test
    public void thisAlwaysPasses() {
        System.out.println("Unit test framework is alive!");
        assertTrue(true);
    }
}
