package org.ohdsi.usagi.indexBuilding;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ohdsi.usagi.BerkeleyDbEngine;
import org.ohdsi.usagi.Concept;
import org.ohdsi.usagi.MapsToRelationship;
import org.ohdsi.usagi.ParentChildRelationShip;
import org.ohdsi.usagi.ui.Global;
import org.ohdsi.utilities.collections.IntHashSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BerkeleyDbBuilderTest {

    @TempDir
    Path tempDir;

    private BerkeleyDbBuilder berkeleyDbBuilder;
    private BerkeleyDbEngine dbEngine;
    private TestProgressReporter testProgressReporter;
    private Path vocabFolder;
    private Path loincFile;

    /**
     * A simple implementation of ProgressReporter for testing purposes.
     * It captures reported messages for verification.
     */
    private static class TestProgressReporter implements ProgressReporter {
        private List<String> reportedMessages = new ArrayList<>();

        @Override
        public void report(String message) {
            reportedMessages.add(message);
            System.out.println("Message: " + message);
        }

        public List<String> getReportedMessages() {
            return reportedMessages;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // Set up the Global folder for the database
        Global.folder = tempDir.toString();

        // Create test progress reporter
        testProgressReporter = new TestProgressReporter();

        // Create the BerkeleyDbBuilder instance
        berkeleyDbBuilder = new BerkeleyDbBuilder();

        // Create a temporary vocabulary folder
        vocabFolder = Files.createDirectory(tempDir.resolve("vocab"));

        // Create test files
        createConceptFile();
        createConceptAncestorFile();
        createConceptRelationshipFile();
        createLoincFile();
    }

    @AfterEach
    void tearDown() {
        // Clean up any open database connections
        if (dbEngine != null) {
            try {
                dbEngine.shutdown();
            } catch (Exception e) {
                // Ignore exceptions during shutdown in tests
            }
        }
    }

    @Test
    void testBuildIndex() {
        // Build the index
        berkeleyDbBuilder.buildIndex(
                vocabFolder.toString(),
                loincFile.toString(),
                testProgressReporter
        );

        // Verify the progress reporter was used to report progress
        assertFalse(testProgressReporter.getReportedMessages().isEmpty(), "Progress reporter should have received progress reports");

        // Open the database for reading
        dbEngine = new BerkeleyDbEngine(Global.folder);
        dbEngine.openForReading();

        // Verify concepts were loaded
        Concept concept = dbEngine.getConcept(123);
        assertNotNull(concept, "Concept should be loaded");
        assertEquals("Test Concept", concept.conceptName, "Concept name should match");
        assertEquals("Test Domain", concept.domainId, "Domain should match");

        // Verify ATC to RxNorm mappings
        Set<Integer> rxNormIds = dbEngine.getRxNormConceptIds("A01BC23");
        assertNotNull(rxNormIds, "RxNorm IDs should be loaded");
        assertEquals(1, rxNormIds.size(), "Should have 1 RxNorm ID");
        assertTrue(rxNormIds.contains(456), "Should contain RxNorm concept ID");

        // Verify parent-child relationships
        List<ParentChildRelationShip> parentRelationships = dbEngine.getParentChildRelationshipsByParentConceptId(123);
        assertNotNull(parentRelationships, "Parent relationships should be loaded");
        assertEquals(1, parentRelationships.size(), "Should have 1 parent relationship");
        assertEquals(456, parentRelationships.get(0).childConceptId, "Child concept ID should match");

        // Verify maps-to relationships
        MapsToRelationship mapsToRelationship = dbEngine.getMapsToRelationship(123);
        assertNotNull(mapsToRelationship, "Maps-to relationship should be loaded");
        assertEquals(456, mapsToRelationship.conceptId2, "Target concept ID should match");
    }

    @Test
    void testLoadValidConceptIdsAndAtcCodes() throws Exception {
        // Use reflection to access the private method
        java.lang.reflect.Method method = BerkeleyDbBuilder.class.getDeclaredMethod(
                "loadValidConceptIdsAndAtcCodes", String.class);
        method.setAccessible(true);

        // Create a new BerkeleyDbBuilder instance and initialize the database
        BerkeleyDbBuilder builder = new BerkeleyDbBuilder();
        dbEngine = new BerkeleyDbEngine(Global.folder);
        dbEngine.createDatabase();

        // Set the dbEngine field using reflection
        java.lang.reflect.Field dbEngineField = BerkeleyDbBuilder.class.getDeclaredField("dbEngine");
        dbEngineField.setAccessible(true);
        dbEngineField.set(builder, dbEngine);

        // Call the method
        IntHashSet validConceptIds = (IntHashSet) method.invoke(builder, vocabFolder.resolve("CONCEPT.csv").toString());

        // Verify the results
        assertNotNull(validConceptIds, "Valid concept IDs should not be null");
        assertEquals(3, validConceptIds.size(), "Should have 3 valid concept IDs");
        assertTrue(validConceptIds.contains(123), "Should contain concept ID 123");
        assertTrue(validConceptIds.contains(456), "Should contain concept ID 456");
        assertTrue(validConceptIds.contains(789), "Should contain concept ID 789");

        // Verify ATC codes were loaded
        java.lang.reflect.Field conceptIdToAtcCodeField = BerkeleyDbBuilder.class.getDeclaredField("conceptIdToAtcCode");
        conceptIdToAtcCodeField.setAccessible(true);
        Map<Integer, String> conceptIdToAtcCode = (Map<Integer, String>) conceptIdToAtcCodeField.get(builder);

        assertNotNull(conceptIdToAtcCode, "ATC code map should not be null");
        assertEquals(1, conceptIdToAtcCode.size(), "Should have 1 ATC code mapping");
        assertEquals("A01BC23", conceptIdToAtcCode.get(789), "ATC code should match");
    }

    @Test
    void testBuildIndexWithZipResource() throws IOException {
        // Prepare the vocabulary folder by unzipping the resource
        Path zipVocabFolder = tempDir.resolve("zip_vocab");
        Files.createDirectories(zipVocabFolder);

        try (ZipInputStream zis = new ZipInputStream(
                getClass().getResourceAsStream("/OMOP-vocabularies-minimal.zip"))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = zipVocabFolder.resolve(entry.getName());
                Files.copy(zis, filePath);
                zis.closeEntry();
            }
        }

        // Set up the Global folder for the database in a separate subfolder
        Path dbFolder = tempDir.resolve("zip_db");
        Files.createDirectories(dbFolder);
        Global.folder = dbFolder.toString();

        // Build the index
        berkeleyDbBuilder.buildIndex(
                zipVocabFolder.toString(),
                null, // No LOINC file
                testProgressReporter
        );

        // Open the database for reading
        dbEngine = new BerkeleyDbEngine(Global.folder);
        dbEngine.openForReading();

        // Verify a known concept from the zip was loaded
        // From our analysis: 1146945	concept.concept_id	Metadata	CDM	Field	S	CDM1
        Concept concept = dbEngine.getConcept(1146945);
        assertNotNull(concept, "Concept 1146945 should be loaded from zip");
        assertEquals("concept.concept_id", concept.conceptName);
        assertEquals("Metadata", concept.domainId);
        assertEquals("CDM", concept.vocabularyId);

        // Verify another concept
        Concept concept2 = dbEngine.getConcept(756315);
        assertNotNull(concept2, "Concept 756315 should be loaded from zip");
        assertEquals("metadata.metadata_type_concept_id", concept2.conceptName);
    }

    // Helper methods to create test files

    private void createConceptFile() throws IOException {
        File conceptFile = vocabFolder.resolve("CONCEPT.csv").toFile();
        try (FileWriter writer = new FileWriter(conceptFile)) {
            writer.write("concept_id\tconcept_name\tdomain_id\tvocabulary_id\tconcept_class_id\tstandard_concept\tconcept_code\tvalid_start_date\tvalid_end_date\tinvalid_reason\n");
            writer.write("123\tTest Concept\tTest Domain\tTest Vocab\tTest Class\tS\tT123\t2020-01-01\t2099-12-31\t\n");
            writer.write("456\tTest Concept 2\tTest Domain\tRxNorm\tTest Class\tS\tT456\t2020-01-01\t2099-12-31\t\n");
            writer.write("789\tTest ATC\tTest Domain\tATC\tTest Class\tS\tA01BC23\t2020-01-01\t2099-12-31\t\n");
            writer.write("999\tInvalid Concept\tTest Domain\tTest Vocab\tTest Class\tS\tT999\t2020-01-01\t2099-12-31\tD\n");
        }
    }

    private void createConceptAncestorFile() throws IOException {
        File ancestorFile = vocabFolder.resolve("CONCEPT_ANCESTOR.csv").toFile();
        try (FileWriter writer = new FileWriter(ancestorFile)) {
            writer.write("ancestor_concept_id\tdescendant_concept_id\tmin_levels_of_separation\tmax_levels_of_separation\n");
            writer.write("123\t456\t1\t1\n");
            writer.write("123\t789\t2\t2\n"); // This one should be ignored (min_levels_of_separation > 1)
            writer.write("123\t123\t0\t0\n"); // This one should be ignored (ancestor = descendant)
        }
    }

    private void createConceptRelationshipFile() throws IOException {
        File relationshipFile = vocabFolder.resolve("CONCEPT_RELATIONSHIP.csv").toFile();
        try (FileWriter writer = new FileWriter(relationshipFile)) {
            writer.write("concept_id_1\tconcept_id_2\trelationship_id\tvalid_start_date\tvalid_end_date\tinvalid_reason\n");
            writer.write("123\t456\tMaps to\t2020-01-01\t2099-12-31\t\n");
            writer.write("789\t456\tATC - RxNorm\t2020-01-01\t2099-12-31\t\n"); // Note: invalid_reason is empty, not null
            writer.write("123\t999\tMaps to\t2020-01-01\t2099-12-31\tD\n"); // This one should be ignored (invalid_reason is not null)
            writer.write("999\t456\tMaps to\t2020-01-01\t2099-12-31\t\n"); // This one should be ignored (concept_id_1 is invalid)
            writer.write("123\t999\tMaps to\t2020-01-01\t2099-12-31\t\n"); // This one should be ignored (concept_id_2 is invalid)
        }
    }

    private void createLoincFile() throws IOException {
        loincFile = tempDir.resolve("LOINC.csv");
        try (FileWriter writer = new FileWriter(loincFile.toFile())) {
            // Note: The LOINC file is read using ReadCSVFileWithHeader, not ReadAthenaFile, so it should be comma-separated
            writer.write("LOINC_NUM,COMPONENT,PROPERTY,TIME_ASPCT,SYSTEM,SCALE_TYP,METHOD_TYP,DefinitionDescription,FORMULA,EXAMPLE_UCUM_UNITS\n");
            writer.write("T123,Test Component,Test Property,Test Time,Test System,Test Scale,Test Method,Test Definition,Test Formula,Test Units\n");
        }
    }
}
