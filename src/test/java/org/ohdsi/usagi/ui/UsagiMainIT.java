package org.ohdsi.usagi.ui;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import org.ohdsi.usagi.CacioTestBase;
import org.ohdsi.usagi.TestUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Please note, the order of the tests is important since the index building has to happen first.
 * The subsequent tests depend on the index already being built.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UsagiMainIT extends CacioTestBase {

    @TempDir
    static Path tempDir;

    private FrameFixture window;
    private org.assertj.swing.core.Robot robot;
    private UsagiMain usagiMain;
    private static Path vocabDir;

    @BeforeAll
    public static void beforeAll() throws IOException {
        // Set working directory to tempDir
        System.setProperty("user.dir", tempDir.toAbsolutePath().toString());

        // Create vocabularyVersion.txt to skip vocab version dialog
        Files.write(tempDir.resolve("vocabularyVersion.txt"), "v5.0".getBytes());

        // Unzip OMOP vocabularies
        vocabDir = tempDir.resolve("vocab");
        Files.createDirectories(vocabDir);
        TestUtils.unzipResource("/OMOP-vocabularies-minimal.zip", vocabDir);
    }

    @BeforeEach
    public void onSetUp() {
        robot = org.assertj.swing.core.BasicRobot.robotWithCurrentAwtHierarchy();
        RebuildIndexDialog.skipSystemExit = true;
    }

    private void setupEnvironment() throws IOException {
        Files.deleteIfExists(tempDir.resolve("authorName.txt"));

        // Prepare UsagiMain
        String[] args = {tempDir.toAbsolutePath().toString()};

        // Launch UsagiMain in a separate thread because initializeUsagi blocks if it shows modal dialogs
        // or if it triggers actions that show modal dialogs.
        new Thread(() -> {
            usagiMain = new UsagiMain(false, args);
            usagiMain.setSkipRebuildIndexAction(false); // Enable it for testing
            usagiMain.initializeUsagi(args);
        }).start();
    }

    @AfterEach
    public void tearDownUsagi() {
        if (window != null) {
            window.cleanUp();
        }
        if (Global.dbEngine != null) {
            Global.dbEngine.shutdown();
        }
    }

    @Test
    @Order(1)
    public void testBuildIndexWithNewAuthor() throws IOException {
        setupEnvironment();

        // 0. Handle Author Dialog
        DialogFixture authorDialog = WindowFinder.findDialog(AuthorDialog.AUTHOR_DIALOG).using(robot);
        authorDialog.target().setLocation(0, 0); // Move to top-left to avoid out of boundaries
        authorDialog.textBox("authorField").setText("New Test Author");
        authorDialog.button("saveButton").click();

        // After clicking Save, initialization continues and the main frame should become visible
        window = WindowFinder.findFrame(new GenericTypeMatcher<JFrame>(JFrame.class) {
            @Override
            protected boolean isMatching(JFrame frame) {
                return frame.isVisible() && frame.getTitle() != null && frame.getTitle().startsWith("Usagi v");
            }
        }).using(robot);
        window.target().setLocation(0, 0); // Move to top-left

        runBuildIndexTest();
    }

    @Test
    @Order(2)
    public void testSearchExactMatch() throws IOException {
        // The index was already built in testBuildIndexWithNewAuthor (Order 1)
        // We just need to launch Usagi and it should find the existing index.
        
        // Ensure authorName.txt exists so we don't get the AuthorDialog
        Files.write(tempDir.resolve("authorName.txt"), "New Test Author".getBytes());

        // Launch UsagiMain
        String[] args = {tempDir.toAbsolutePath().toString()};
        new Thread(() -> {
            usagiMain = new UsagiMain(false, args);
            usagiMain.setSkipRebuildIndexAction(false);
            usagiMain.initializeUsagi(args);
        }).start();

        // Wait for the main frame to become visible
        window = WindowFinder.findFrame("UsagiMainFrame").using(robot);
        window.target().setLocation(0, 0); // Move to top-left

        // 6. Enter search term "Deutsch"
        window.textBox("manualQueryField").setText("Deutsch");

        // 7. Verify first result
        // The search is asynchronous, so we might need to wait.
        // AssertJ Swing's table(name) provides assertions.
        org.assertj.swing.fixture.JTableFixture searchTable = window.table("searchResultsTable");
        
        // Wait until at least one row is present
        org.assertj.swing.timing.Pause.pause(new org.assertj.swing.timing.Condition("Wait for search results") {
            @Override
            public boolean test() {
                return searchTable.rowCount() > 0;
            }
        }, org.assertj.swing.timing.Timeout.timeout(5000));

        // In ConceptTableModel: col 0 is Score (Double), col 2 is Concept ID (Integer)
        searchTable.requireCellValue(org.assertj.swing.data.TableCell.row(0).column(0), "1.00");
        searchTable.requireCellValue(org.assertj.swing.data.TableCell.row(0).column(2), "41990102");
    }

    @Test
    @Order(3)
    public void testSearchWithTypo() throws IOException {
        // Ensure authorName.txt exists so we don't get the AuthorDialog
        Files.write(tempDir.resolve("authorName.txt"), "New Test Author".getBytes());

        // Launch UsagiMain
        String[] args = {tempDir.toAbsolutePath().toString()};
        new Thread(() -> {
            usagiMain = new UsagiMain(false, args);
            usagiMain.setSkipRebuildIndexAction(false);
            usagiMain.initializeUsagi(args);
        }).start();

        // Wait for the main frame to become visible
        window = WindowFinder.findFrame("UsagiMainFrame").using(robot);
        window.target().setLocation(0, 0); // Move to top-left

        // Enter search term "Deutsh" (typo for Deutsch)
        window.textBox("manualQueryField").setText("Deutsh");

        // Verify first result
        org.assertj.swing.fixture.JTableFixture searchTable = window.table("searchResultsTable");
        
        // Wait until at least one row is present
        org.assertj.swing.timing.Pause.pause(new org.assertj.swing.timing.Condition("Wait for search results") {
            @Override
            public boolean test() {
                return searchTable.rowCount() > 0;
            }
        }, org.assertj.swing.timing.Timeout.timeout(5000));

        // In ConceptTableModel: col 0 is Score (Double), col 2 is Concept ID (Integer)
        // For "Deutsh", the score is expected to be lower than 1.00. Observed score: 0.75
        searchTable.requireCellValue(org.assertj.swing.data.TableCell.row(0).column(0), "0.75");
        searchTable.requireCellValue(org.assertj.swing.data.TableCell.row(0).column(2), "41990102");
    }

    @Test
    @Order(4)
    public void testImportCodes() throws IOException {
        // Ensure authorName.txt exists so we don't get the AuthorDialog
        Files.write(tempDir.resolve("authorName.txt"), "New Test Author".getBytes());

        // Create a small CSV file for the test
        Path csvPath = tempDir.resolve("test_import.csv");
        String csvContent = "source_code,source_name,frequency\n" +
                "C1,Deutsch,10\n" +
                "C2,English,20\n";
        Files.write(csvPath, csvContent.getBytes());

        // Launch UsagiMain
        String[] args = {tempDir.toAbsolutePath().toString()};
        new Thread(() -> {
            usagiMain = new UsagiMain(false, args);
            usagiMain.setSkipRebuildIndexAction(false);
            usagiMain.initializeUsagi(args);
        }).start();

        // Wait for the main frame to become visible
        window = WindowFinder.findFrame("UsagiMainFrame").using(robot);
        window.target().setLocation(0, 0); // Move to top-left

        // Open ImportDialog manually to avoid JFileChooser
        String absolutePath = csvPath.toAbsolutePath().toString();
        new Thread(() -> {
            usagiMain.setSkipRebuildIndexAction(true); // Don't let it pop up anything else
            ImportDialog dialog = new ImportDialog(absolutePath);
        }).start();

        // Find the ImportDialog
        DialogFixture importDialog = WindowFinder.findDialog(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return dialog.isVisible() && dialog.getTitle().startsWith("Import codes from");
            }
        }).using(robot);
        importDialog.target().setLocation(0, 0); // Move to top-left to stay in bounds
        importDialog.target().setSize(800, 600); // Smaller to ensure it fits and button is visible
        importDialog.target().validate();

        // Configure mappings in ImportDialog
        // Column mappings are in JComboBoxes.
        // sourceCodeColumn, sourceNameColumn, sourceFrequencyColumn
        importDialog.comboBox(new GenericTypeMatcher<JComboBox>(JComboBox.class) {
            @Override
            protected boolean isMatching(JComboBox comboBox) {
                return "The column containing the source code".equals(comboBox.getToolTipText());
            }
        }).selectItem("source_code");

        importDialog.comboBox(new GenericTypeMatcher<JComboBox>(JComboBox.class) {
            @Override
            protected boolean isMatching(JComboBox comboBox) {
                return "The column containing the name or description of the source code, which will be used for matching".equals(comboBox.getToolTipText());
            }
        }).selectItem("source_name");

        importDialog.comboBox(new GenericTypeMatcher<JComboBox>(JComboBox.class) {
            @Override
            protected boolean isMatching(JComboBox comboBox) {
                return "The column containing the frequency of the code in the source database".equals(comboBox.getToolTipText());
            }
        }).selectItem("frequency");

        // Click 'Import' button
        importDialog.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(JButton button) {
                return "Import".equals(button.getText());
            }
        }).click();

        // The import happens in a background thread (ImportCodesThread).
        // We need to wait for it to complete. The ImportDialog should close.
        org.assertj.swing.timing.Pause.pause(new org.assertj.swing.timing.Condition("Wait for ImportDialog to close") {
            @Override
            public boolean test() {
                return !importDialog.target().isVisible();
            }
        }, org.assertj.swing.timing.Timeout.timeout(10000));

        // Verify the mapping table in UsagiMainFrame has 2 rows
        // We need to find the table in MappingTablePanel. It's an UsagiTable.
        org.assertj.swing.fixture.JTableFixture mappingTable = window.table(new GenericTypeMatcher<JTable>(JTable.class) {
            @Override
            protected boolean isMatching(JTable table) {
                // The main mapping table has many columns (around 21 by default, but some might be hidden)
                // UsagiTable[name=null, rowCount=2, columnCount=18, ...] seems to be the one.
                return table.isShowing() && table.getColumnCount() > 15;
            }
        });

        // Wait for rows to appear (matching is async)
        org.assertj.swing.timing.Pause.pause(new org.assertj.swing.timing.Condition("Wait for mapping results") {
            @Override
            public boolean test() {
                return mappingTable.rowCount() == 2;
            }
        }, org.assertj.swing.timing.Timeout.timeout(10000));

        // Check content of the first row
        // Columns: "Status", "Source code", "Source term", "Frequency", "Match score", ...
        // Index of "Source code" is 1, "Source term" is 2.
        // two results are expected: {C1, Deutsch}, {C2, English}, but the order seems not deterministic,
        // so we expect either order here
        String[][] expectedRows = {{"C1", "Deutsch"}, {"C2", "English"}};
        String[][] actualRows = {
                {mappingTable.valueAt(org.assertj.swing.data.TableCell.row(0).column(1)), mappingTable.valueAt(org.assertj.swing.data.TableCell.row(0).column(2))},
                {mappingTable.valueAt(org.assertj.swing.data.TableCell.row(1).column(1)), mappingTable.valueAt(org.assertj.swing.data.TableCell.row(1).column(2))}
        };

        java.util.Arrays.sort(actualRows, java.util.Comparator.comparing(r -> r[0]));
        org.junit.jupiter.api.Assertions.assertArrayEquals(expectedRows, actualRows);
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
}
