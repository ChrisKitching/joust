package tests.unittests;

import static org.junit.Assert.*;

import joust.utils.LogUtils;
import lombok.experimental.ExtensionMethod;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.logging.Logger;

@Log
@ExtensionMethod({Logger.class, LogUtils.LogExtensions.class})
@RunWith(JUnit4.class)
public class TestTest {
    @Test
    public void thisAlwaysPasses() {
        log.info("Unit test framework is alive!");
        assertTrue(true);
    }
}
