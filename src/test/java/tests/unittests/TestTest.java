package tests.unittests;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestTest {
    private static Logger logger = LogManager.getLogger();

    @Test
    public void thisAlwaysPasses() {
        logger.info("Unit test framework is alive!");
        assertTrue(true);
    }
}
