package org.ohdsi.usagi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sleepycat.persist.EntityCursor;

import static org.junit.jupiter.api.Assertions.*;

class BerkeleyDbEngineTest {

    @TempDir
    Path tempDir;

    private BerkeleyDbEngine dbEngine;
    private String testFolder;

    @BeforeEach
    void setUp() {
        testFolder = tempDir.toString();
        dbEngine = new BerkeleyDbEngine(testFolder);
    }

    @AfterEach
    void tearDown() {
        try {
            dbEngine.shutdown();
        } catch (Exception e) {
            // Ignore exceptions during shutdown in tests
        }
    }

    @Test
    void testCreateDatabase() {
        // Create the database
        dbEngine.createDatabase();

        // Verify the database folder was created
        File dbFolder = new File(testFolder + "/" + BerkeleyDbEngine.DATABASE_FOLDER);
        assertTrue(dbFolder.exists(), "Database folder should exist after creation");
        assertTrue(dbFolder.isDirectory(), "Database folder should be a directory");
    }

    @Test
    void testPutAndGetConcept() {
        // Create the database
        dbEngine.createDatabase();

        // Create a test concept
        Concept concept = new Concept();
        concept.conceptId = 123;
        concept.conceptName = "Test Concept";
        concept.domainId = "Test Domain";
        concept.vocabularyId = "Test Vocabulary";
        concept.conceptClassId = "Test Class";
        concept.standardConcept = "S";
        concept.conceptCode = "T123";
        concept.validStartDate = "2020-01-01";
        concept.validEndDate = "2099-12-31";
        concept.invalidReason = "";

        // Put the concept in the database
        dbEngine.put(concept);

        // Get the concept from the database
        Concept retrievedConcept = dbEngine.getConcept(123);

        // Verify the retrieved concept matches the original
        assertNotNull(retrievedConcept, "Retrieved concept should not be null");
        assertEquals(123, retrievedConcept.conceptId, "Concept ID should match");
        assertEquals("Test Concept", retrievedConcept.conceptName, "Concept name should match");
        assertEquals("Test Domain", retrievedConcept.domainId, "Domain ID should match");
        assertEquals("Test Vocabulary", retrievedConcept.vocabularyId, "Vocabulary ID should match");
        assertEquals("Test Class", retrievedConcept.conceptClassId, "Concept class ID should match");
        assertEquals("S", retrievedConcept.standardConcept, "Standard concept flag should match");
        assertEquals("T123", retrievedConcept.conceptCode, "Concept code should match");
        assertEquals("2020-01-01", retrievedConcept.validStartDate, "Valid start date should match");
        assertEquals("2099-12-31", retrievedConcept.validEndDate, "Valid end date should match");
        assertEquals("", retrievedConcept.invalidReason, "Invalid reason should match");
    }

    @Test
    void testPutAndGetMapsToRelationship() {
        // Create the database
        dbEngine.createDatabase();

        // Create a test relationship
        MapsToRelationship relationship = new MapsToRelationship();
        relationship.conceptId1 = 123;
        relationship.conceptId2 = 456;

        // Put the relationship in the database
        dbEngine.put(relationship);

        // Get the relationship from the database
        MapsToRelationship retrievedRelationship = dbEngine.getMapsToRelationship(123);

        // Verify the retrieved relationship matches the original
        assertNotNull(retrievedRelationship, "Retrieved relationship should not be null");
        assertEquals(123, retrievedRelationship.conceptId1, "Concept ID 1 should match");
        assertEquals(456, retrievedRelationship.conceptId2, "Concept ID 2 should match");

        // Test getting relationships by conceptId2
        List<MapsToRelationship> relationships = dbEngine.getMapsToRelationshipsByConceptId2(456);
        assertEquals(1, relationships.size(), "Should find one relationship");
        assertEquals(123, relationships.get(0).conceptId1, "Concept ID 1 should match");
        assertEquals(456, relationships.get(0).conceptId2, "Concept ID 2 should match");
    }

    @Test
    void testPutAndGetAtcToRxNorm() {
        // Create the database
        dbEngine.createDatabase();

        // Add ATC to RxNorm mappings
        String atcCode = "A01BC23";
        dbEngine.putAtcToRxNorm(atcCode, 123);
        dbEngine.putAtcToRxNorm(atcCode, 456);

        // Get the RxNorm concept IDs for the ATC code
        Set<Integer> conceptIds = dbEngine.getRxNormConceptIds(atcCode);

        // Verify the retrieved concept IDs
        assertNotNull(conceptIds, "Retrieved concept IDs should not be null");
        assertEquals(2, conceptIds.size(), "Should have 2 concept IDs");
        assertTrue(conceptIds.contains(123), "Should contain concept ID 123");
        assertTrue(conceptIds.contains(456), "Should contain concept ID 456");

        // Test getting concept IDs for a non-existent ATC code
        Set<Integer> emptySet = dbEngine.getRxNormConceptIds("NONEXISTENT");
        assertNotNull(emptySet, "Should return an empty set, not null");
        assertTrue(emptySet.isEmpty(), "Set should be empty for non-existent ATC code");
    }

    @Test
    void testPutAndGetParentChildRelationship() {
        // Create the database
        dbEngine.createDatabase();

        // Create a test parent-child relationship
        ParentChildRelationShip relationship = new ParentChildRelationShip();
        relationship.parentConceptId = 123;
        relationship.childConceptId = 456;

        // Put the relationship in the database
        dbEngine.put(relationship);

        // Get relationships by parent concept ID
        List<ParentChildRelationShip> parentRelationships = dbEngine.getParentChildRelationshipsByParentConceptId(123);

        // Verify the retrieved relationships
        assertNotNull(parentRelationships, "Retrieved parent relationships should not be null");
        assertEquals(1, parentRelationships.size(), "Should have 1 parent relationship");
        assertEquals(123, parentRelationships.get(0).parentConceptId, "Parent concept ID should match");
        assertEquals(456, parentRelationships.get(0).childConceptId, "Child concept ID should match");

        // Get relationships by child concept ID
        List<ParentChildRelationShip> childRelationships = dbEngine.getParentChildRelationshipsByChildConceptId(456);

        // Verify the retrieved relationships
        assertNotNull(childRelationships, "Retrieved child relationships should not be null");
        assertEquals(1, childRelationships.size(), "Should have 1 child relationship");
        assertEquals(123, childRelationships.get(0).parentConceptId, "Parent concept ID should match");
        assertEquals(456, childRelationships.get(0).childConceptId, "Child concept ID should match");
    }

    @Test
    void testGetStats() {
        // Create the database
        dbEngine.createDatabase();

        // Add some test data
        Concept concept = new Concept();
        concept.conceptId = 123;
        dbEngine.put(concept);

        MapsToRelationship mapsToRelationship = new MapsToRelationship();
        mapsToRelationship.conceptId1 = 123;
        mapsToRelationship.conceptId2 = 456;
        dbEngine.put(mapsToRelationship);

        ParentChildRelationShip parentChildRelationship = new ParentChildRelationShip();
        parentChildRelationship.parentConceptId = 123;
        parentChildRelationship.childConceptId = 456;
        dbEngine.put(parentChildRelationship);

        // Get the stats
        BerkeleyDbEngine.BerkeleyDbStats stats = dbEngine.getStats();

        // Verify the stats
        assertEquals(1, stats.conceptCount, "Should have 1 concept");
        assertEquals(1, stats.mapsToRelationshipCount, "Should have 1 maps-to relationship");
        assertEquals(1, stats.parentChildCount, "Should have 1 parent-child relationship");
    }

    @Test
    void testOpenForReading() {
        // Create the database
        dbEngine.createDatabase();

        // Add a test concept
        Concept concept = new Concept();
        concept.conceptId = 123;
        concept.conceptName = "Test Concept";
        dbEngine.put(concept);

        // Shutdown the database
        dbEngine.shutdown();

        // Create a new database engine and open for reading
        BerkeleyDbEngine readOnlyEngine = new BerkeleyDbEngine(testFolder);
        readOnlyEngine.openForReading();

        // Verify we can read the concept
        Concept retrievedConcept = readOnlyEngine.getConcept(123);
        assertNotNull(retrievedConcept, "Should be able to retrieve the concept");
        assertEquals("Test Concept", retrievedConcept.conceptName, "Concept name should match");

        // Clean up
        readOnlyEngine.shutdown();
    }

    @Test
    void testGetConceptCursor() {
        // Create the database
        dbEngine.createDatabase();

        // Add some test concepts
        Concept concept1 = new Concept();
        concept1.conceptId = 123;
        concept1.conceptName = "Test Concept 1";
        dbEngine.put(concept1);

        Concept concept2 = new Concept();
        concept2.conceptId = 456;
        concept2.conceptName = "Test Concept 2";
        dbEngine.put(concept2);

        // Get a cursor for all concepts
        EntityCursor<Concept> cursor = dbEngine.getConceptCursor();

        // Count the concepts and verify their IDs
        Set<Integer> conceptIds = new HashSet<>();
        try {
            for (Concept concept : cursor) {
                conceptIds.add(concept.conceptId);
            }
        } finally {
            cursor.close();
        }

        assertEquals(2, conceptIds.size(), "Should have 2 concepts");
        assertTrue(conceptIds.contains(123), "Should contain concept ID 123");
        assertTrue(conceptIds.contains(456), "Should contain concept ID 456");
    }
}
