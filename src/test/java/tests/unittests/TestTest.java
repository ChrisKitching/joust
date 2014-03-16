package tests.unittests;

import static org.junit.Assert.*;

import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Log4j2
@RunWith(JUnit4.class)
public class TestTest {
    @Test
    public void thisAlwaysPasses() {
        log.info("Unit test framework is alive!");
        assertTrue(true);
    }
}
