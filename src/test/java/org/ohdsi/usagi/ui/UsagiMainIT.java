package org.ohdsi.usagi.ui;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CacioTest
public class UsagiMainIT {
    private final static int WIDTH = 1920;
    private final static int HEIGHT = 1080;

    @TempDir
    Path tempDir;

    private FrameFixture window;
    private org.assertj.swing.core.Robot robot;
    private UsagiMain usagiMain;
    private Path vocabDir;

    @BeforeAll
    public static void setupOnce() {
        System.setProperty("cacio.managed.screensize", String.format("%sx%s", WIDTH, HEIGHT));
    }

    @BeforeEach
    public void onSetUp() {
        robot = org.assertj.swing.core.BasicRobot.robotWithCurrentAwtHierarchy();
        RebuildIndexDialog.skipSystemExit = true;

        // Set working directory to tempDir
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
    }

    private void setupEnvironment(boolean createAuthorFile) throws IOException {
        if (createAuthorFile) {
            // Create author name file to skip author dialog
            Files.write(tempDir.resolve("authorName.txt"), "Test Author".getBytes());
        }

        // Unzip OMOP vocabularies
        vocabDir = tempDir.resolve("vocab");
        Files.createDirectories(vocabDir);
        unzipResource("/OMOP-vocabularies-minimal.zip", vocabDir);

        // Prepare UsagiMain
        String[] args = {tempDir.toAbsolutePath().toString()};

        // Launch UsagiMain in a separate thread because initializeUsagi blocks if it shows modal dialogs
        // or if it triggers actions that show modal dialogs.
        new Thread(() -> {
            usagiMain = new UsagiMain(false, args);
            usagiMain.setSkipRebuildIndexAction(false); // Enable it for testing
            usagiMain.initializeUsagi(args);
        }).start();

        if (createAuthorFile) {
            // If author file exists, we expect the main frame to become visible directly (after initialization)
            window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
                @Override
                protected boolean isMatching(JFrame frame) {
                    return frame.isVisible() && frame.getTitle() != null && frame.getTitle().startsWith("Usagi v");
                }
            }).using(robot);
        } else {
            // If no author file, the AuthorDialog (modal) is shown, blocking initialization and frame visibility.
            // We don't wait for the frame here.
        }
    }

    @AfterEach
    public void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
        if (Global.dbEngine != null) {
            Global.dbEngine.shutdown();
        }
        // Clean up authorName.txt if it exists to not affect other tests
        try {
            Files.deleteIfExists(tempDir.resolve("authorName.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBuildIndexWithExistingAuthor() throws IOException {
        setupEnvironment(true);
        runBuildIndexTest();
    }

    @Test
    public void testBuildIndexWithNewAuthor() throws IOException {
        setupEnvironment(false);

        // 0. Handle Author Dialog
        DialogFixture authorDialog = WindowFinder.findDialog(AuthorDialog.AUTHOR_DIALOG).using(robot);
        authorDialog.textBox("authorField").setText("New Test Author");
        authorDialog.button("saveButton").click();

        // After clicking Save, initialization continues and the main frame should become visible
        window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
            @Override
            protected boolean isMatching(JFrame frame) {
                return frame.isVisible() && frame.getTitle() != null && frame.getTitle().startsWith("Usagi v");
            }
        }).using(robot);

        runBuildIndexTest();
    }

    private void runBuildIndexTest() {
        // 1. Open Rebuild Index Dialog
        // The dialog should have opened automatically because main index does not exist.
        DialogFixture rebuildDialog = WindowFinder.findDialog(RebuildIndexDialog.REBUILD_INDEX_DIALOG).using(robot);

        // Verify working directory is set correctly
        assertTrue(System.getProperty("user.dir").equals(tempDir.toAbsolutePath().toString()), "Working directory should be set to tempDir");

        // 2. Fill vocabulary directory and build index
        // The vocabDir points to the directory where the ZIP was unzipped.
        rebuildDialog.textBox("vocabFolderField").setText(vocabDir.toAbsolutePath().toString());

        // 3. Click "Build index" button
        rebuildDialog.button("buildIndexButton").click();

        // The build process is asynchronous (BuildThread).
        // We wait for the JOptionPane "Please restart Usagi"
        JOptionPaneFixture restartPane = window.optionPane();

        // Before clicking OK, check if files exist.
        File sleepyCatDir = new File(tempDir.toFile(), "sleepyCat");
        File mainIndexDir = new File(tempDir.toFile(), "mainIndex");

        assertTrue(sleepyCatDir.exists() && sleepyCatDir.isDirectory(), "sleepyCat directory should exist");
        assertTrue(mainIndexDir.exists() && mainIndexDir.isDirectory(), "mainIndex directory should exist");

        // Check for some files inside
        assertTrue(new File(sleepyCatDir, "00000000.jdb").exists() || (sleepyCatDir.list() != null && sleepyCatDir.list().length > 0), "sleepyCat should not be empty");
        assertTrue(mainIndexDir.list() != null && mainIndexDir.list().length > 0, "mainIndex should not be empty");

        // 4. Click OK on the restart dialog
        restartPane.okButton().click();
    }

    private void unzipResource(String resourceName, Path targetDir) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceName);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir.toFile(), entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
}
