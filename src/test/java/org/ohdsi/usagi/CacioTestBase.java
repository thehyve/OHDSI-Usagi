package org.ohdsi.usagi;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/*
 * Base class for Cacio tests, providing common setup and configuration for
 * unit/integration tests that use the Cacio virtual screen to run Swing tests.
 */
@CacioTest
public abstract class CacioTestBase extends AssertJSwingJUnitTestCase {
    protected final static int WIDTH = 1920;
    protected final static int HEIGHT = 1080;

    @BeforeAll
    public static void setupCacio() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    @Override
    protected void onSetUp() {
        robot().settings().delayBetweenEvents(100);
        // Set default timeouts to be more generous to avoid timing issues in CI environments
        robot().settings().timeoutToBeVisible(10000);
        robot().settings().timeoutToFindPopup(10000);
        robot().cleanUp();
    }
}
