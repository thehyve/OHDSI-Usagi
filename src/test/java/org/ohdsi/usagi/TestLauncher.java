package org.ohdsi.usagi;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioAssertJRunner;
import org.assertj.swing.driver.ComponentShownWaiter;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import javax.swing.*;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

import org.ohdsi.usagi.ui.AboutDialog;
import org.ohdsi.usagi.ui.AuthorDialog;
import org.ohdsi.usagi.ui.UsagiMain;

/*
 * CacioTestRunner enables running the Swing GUI tests in a virtual screen. This allows the integration tests to run
 * anywhere without being blocked by the absence of a real screen (e.g. github actions), and without being
 * disrupted by unrelated user activity on workstations/laptops (any keyboard or mouse action).
 * For debugging purposes, you can disable the annotation below to have the tests run on your screen. Be aware that
 * any interaction with mouse or keyboard can (will) disrupt the tests if they run on your screen.
 */
@RunWith(CacioAssertJRunner.class)
public class TestLauncher {
    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;

    public static void main(String[] args) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));
        Result result = junit.run(TestLauncher.class);
        if (!result.wasSuccessful()) {
            System.out.println("test failed!");
        }
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    @BeforeClass
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    private FrameFixture window;
    private UsagiMain usagiMain;

    @Before
    public void onSetUp() {
        String[] args = {};
        usagiMain = GuiActionRunner.execute(() -> new UsagiMain(false, args));
        usagiMain.setSkipRebuildIndexAction(true);
        window = new FrameFixture(usagiMain.getFrame());
    }

    @Test
    public void closeRIAH() throws InterruptedException {
        //robot().settings().delayBetweenEvents(5000);
        //
        // A dialog will be opened during initialization, before the RIAH application becomes available
        // for interaction. Since the only goal of this test is confirming that the application can be started,
        // becomes responsive, and can be stopped, this dialog should be ignored (closed). The thread
        // below takes care of this. It has to be a separate thread because otherwise UsagiMain.initializeUsagi(args)
        // will block with this dialog open while the test is not yet able to interact with the application.
        //
        String[] args = {};
        new Thread(() -> {
            ComponentShownWaiter.waitTillShown(window.dialog(AuthorDialog.AUTHOR_DIALOG).target());
            DialogFixture authorDialog = window.dialog(AuthorDialog.AUTHOR_DIALOG);
            authorDialog.close();
        }).start();
        try {
            SwingUtilities.invokeAndWait(() -> usagiMain.initializeUsagi(args)); // needs to happen on the EDT
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        window.show();
        window.menuItemWithPath("Help|" + AboutDialog.VERSION).click();
        DialogFixture aboutDialog = window.dialog(AboutDialog.ABOUT_DIALOG);
        JTextComponentFixture aboutTextComponent = aboutDialog.textBox(AboutDialog.ABOUT_TEXT);
        assertTrue(aboutTextComponent.text().contains(AboutDialog.ABOUT_TEXT_START));
        aboutDialog.close();
        window.menuItemWithPath("File|Exit").click();
        // AssertJ/Swing does not seem to function well if we actually close the application,
        // so do verify that the confirmation window is visible, but do not click it, and let AssertJ/Swing take
        // care of actually closing the application.
        assertNotNull(window.optionPane().buttonWithText("Yes"));
    }
}