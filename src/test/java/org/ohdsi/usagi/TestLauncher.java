package org.ohdsi.usagi;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.driver.ComponentShownWaiter;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import javax.swing.*;

import java.lang.reflect.InvocationTargetException;

import org.ohdsi.usagi.ui.AboutDialog;
import org.ohdsi.usagi.ui.AuthorDialog;
import org.ohdsi.usagi.ui.GUITestExtension;
import org.ohdsi.usagi.ui.UsagiMain;

@ExtendWith(GUITestExtension.class)
@CacioTest  // causes test(s) to run in a virtual display environment; disable this to see the test(s) run on your screen
public class TestLauncher {
    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;

    /**
     * Main method to run the tests in this class.
     * This is used to run the tests from an IDE or command line.
     * It uses JUnit Platform Launcher to discover and execute tests.
     */
    public static void main(String[] args) throws InterruptedException {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(TestLauncher.class))
                .build();
        org.junit.platform.launcher.Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }

    @BeforeAll
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    private FrameFixture window;
    private UsagiMain usagiMain;

    @BeforeEach
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