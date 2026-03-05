package org.ohdsi.usagi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.ohdsi.usagi.ui.Global;
import org.ohdsi.utilities.DirectoryUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class UsagiSearchEngineTest {

    @TempDir
    Path tempDir;

    private UsagiSearchEngine searchEngine;
    private String testFolder;

    @BeforeEach
    void setUp() throws IOException {
        testFolder = tempDir.toString();
        Global.folder = testFolder;
        Global.dbEngine = new BerkeleyDbEngine(testFolder);
        Global.dbEngine.createDatabase();
        searchEngine = new UsagiSearchEngine(testFolder);
    }

    @AfterEach
    void tearDown() {
        if (searchEngine != null) {
            searchEngine.close();
        }
        if (Global.dbEngine != null) {
            Global.dbEngine.shutdown();
        }
    }

    @Test
    void testCreateNewMainIndex() {
        // Initially, the index should not exist
        assertFalse(searchEngine.mainIndexExists(), "Main index should not exist initially");

        // Create the main index
        searchEngine.createNewMainIndex();

        // Verify the main index folder was created
        File indexFolder = new File(testFolder + "/" + UsagiSearchEngine.MAIN_INDEX_FOLDER);
        assertTrue(indexFolder.exists(), "Main index folder should exist after creation");
        assertTrue(indexFolder.isDirectory(), "Main index folder should be a directory");

        // Verify mainIndexExists() returns true
        assertTrue(searchEngine.mainIndexExists(), "mainIndexExists() should return true after creation");

        // Verify we can add a term to the index
        Concept concept = new Concept();
        concept.conceptId = 1;
        concept.conceptName = "Test Concept";
        concept.domainId = "Condition";
        concept.vocabularyId = "SNOMED";
        concept.conceptClassId = "Clinical Finding";
        concept.standardConcept = "S";

        assertDoesNotThrow(() -> {
            searchEngine.addTermToIndex("Test Term", UsagiSearchEngine.CONCEPT_TERM, concept);
        }, "Adding a term to the index should not throw an exception");

        // Close and reopen to verify we can search (basic verification that index is valid)
        searchEngine.close();
        searchEngine.openIndexForSearching(false);
        assertTrue(searchEngine.isOpenForSearching(), "Search engine should be open for searching");
        assertEquals(1, searchEngine.getTermCount(), "Term count should be 1");
    }
}
