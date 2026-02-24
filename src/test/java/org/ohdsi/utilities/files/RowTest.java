package org.ohdsi.utilities.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RowTest {

    private Row emptyRow;
    private Row populatedRow;
    private List<String> testCells;
    private Map<String, Integer> testFieldMap;

    @BeforeEach
    void setUp() {
        // Create an empty row
        emptyRow = new Row();

        // Create test data for a populated row
        testCells = new ArrayList<>();
        testCells.add("value1");
        testCells.add("123");
        testCells.add("456.789");
        testCells.add("true");
        testCells.add("");

        testFieldMap = new HashMap<>();
        testFieldMap.put("field1", 0);
        testFieldMap.put("field2", 1);
        testFieldMap.put("field3", 2);
        testFieldMap.put("field4", 3);
        testFieldMap.put("field5", 4);
        testFieldMap.put("field6", 5);

        // Create a populated row
        populatedRow = new Row(testCells, testFieldMap);
    }

    @Test
    void testEmptyConstructor() {
        assertEquals(0, emptyRow.size(), "New row should have size 0");
        assertEquals(0, emptyRow.getCells().size(), "New row should have 0 cells");
        assertEquals(0, emptyRow.getFieldNames().size(), "New row should have 0 field names");
    }

    @Test
    void testConstructorWithData() {
        assertEquals(5, populatedRow.size(), "Row should have 5 cells");
        assertEquals(6, populatedRow.getFieldNames().size(), "Row should have 6 field names");
        assertEquals("value1", populatedRow.get("field1"), "field1 should have value 'value1'");
        assertEquals("123", populatedRow.get("field2"), "field2 should have value '123'");
    }

    @Test
    void testCopyConstructor() {
        // Create a copy of the populated row
        Row copiedRow = new Row(populatedRow);

        // Verify the copy has the same data
        assertEquals(populatedRow.size(), copiedRow.size(), "Copied row should have same size");
        assertEquals(populatedRow.get("field1"), copiedRow.get("field1"), "field1 should match");
        assertEquals(populatedRow.get("field2"), copiedRow.get("field2"), "field2 should match");

        // Modify the original row
        populatedRow.set("field1", "modified");

        // Verify the copy is independent
        assertNotEquals(populatedRow.get("field1"), copiedRow.get("field1"), "Modifying original should not affect copy");
    }

    @Test
    void testGetWithDefaultValue() {
        // Test getting existing field
        assertEquals("value1", populatedRow.get("field1", "default"), "Should return actual value for existing field");

        // Test getting non-existent field
        assertEquals("default", populatedRow.get("nonExistentField", "default"), "Should return default value for non-existent field");

        // Test getting empty field
        assertEquals("default", populatedRow.get("field5", "default"), "Should return default value for empty field");
    }

    @Test
    void testGetThrowsExceptionForNonExistentField() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            emptyRow.get("nonExistentField");
        }, "Should throw RuntimeException for non-existent field");

        assertTrue(exception.getMessage().contains("not found"), "Exception message should indicate field not found");
    }

    @Test
    void testGetInt() {
        assertEquals(123, populatedRow.getInt("field2"), "getInt should parse integer correctly");

        // Test with default value
        assertEquals(999, populatedRow.getInt("nonExistentField", "999"), "getInt with default should work for non-existent field");
    }

    @Test
    void testGetLong() {
        assertEquals(123L, populatedRow.getLong("field2"), "getLong should parse long correctly");

        // Test with default value
        assertEquals(999L, populatedRow.getLong("nonExistentField", "999"), "getLong with default should work for non-existent field");
    }

    @Test
    void testGetDouble() {
        assertEquals(456.789, populatedRow.getDouble("field3"), "getDouble should parse double correctly");

        // Test with default value
        assertEquals(999.5, populatedRow.getDouble("nonExistentField", "999.5"), "getDouble with default should work for non-existent field");
    }

    @Test
    void testAdd() {
        // Add string value
        emptyRow.add("newField1", "newValue");
        assertEquals("newValue", emptyRow.get("newField1"), "add(String, String) should work");
        assertEquals(1, emptyRow.size(), "Size should be updated after add");

        // Add int value
        emptyRow.add("newField2", 123);
        assertEquals("123", emptyRow.get("newField2"), "add(String, int) should work");

        // Add boolean value
        emptyRow.add("newField3", true);
        assertEquals("true", emptyRow.get("newField3"), "add(String, boolean) should work");

        // Add double value
        emptyRow.add("newField4", 456.789);
        assertEquals("456.789", emptyRow.get("newField4"), "add(String, double) should work");

        // Add long value
        emptyRow.add("newField5", 123456789L);
        assertEquals("123456789", emptyRow.get("newField5"), "add(String, long) should work");
    }

    @Test
    void testSet() {
        // Set string value
        populatedRow.set("field1", "newValue");
        assertEquals("newValue", populatedRow.get("field1"), "set(String, String) should update value");

        // Set int value
        populatedRow.set("field2", 999);
        assertEquals("999", populatedRow.get("field2"), "set(String, int) should update value");

        // Set long value
        populatedRow.set("field3", 999L);
        assertEquals("999", populatedRow.get("field3"), "set(String, long) should update value");

        // Set double value
        populatedRow.set("field4", 999.5);
        assertEquals("999.5", populatedRow.get("field4"), "set(String, double) should update value");
    }

    @Test
    void testGetCells() {
        List<String> cells = populatedRow.getCells();
        assertEquals(5, cells.size(), "getCells should return all cells");
        assertEquals("value1", cells.get(0), "First cell should match");
        assertEquals("123", cells.get(1), "Second cell should match");
    }

    @Test
    void testGetFieldNames() {
        List<String> fieldNames = populatedRow.getFieldNames();
        assertEquals(6, fieldNames.size(), "getFieldNames should return all field names");
        assertTrue(fieldNames.contains("field1"), "Field names should include field1");
        assertTrue(fieldNames.contains("field2"), "Field names should include field2");
    }

    @Test
    void testToString() {
        String rowString = populatedRow.toString();
        assertTrue(rowString.contains("field1"), "toString should include field names");
        assertTrue(rowString.contains("value1"), "toString should include values");
    }

    @Test
    void testRemove() {
        // Remove a field
        populatedRow.remove("field1");

        // Verify field was removed
        Exception exception = assertThrows(RuntimeException.class, () -> {
            populatedRow.get("field1");
        }, "Should throw exception after field is removed");

        // Verify size was updated
        assertEquals(4, populatedRow.size(), "Size should be updated after remove");

        // Verify indices were adjusted
        assertEquals("123", populatedRow.get("field2"), "field2 should still be accessible");
    }

    @Test
    void testSize() {
        assertEquals(0, emptyRow.size(), "Empty row should have size 0");
        assertEquals(5, populatedRow.size(), "Populated row should have size 5");

        // Add a field and verify size increases
        emptyRow.add("newField", "value");
        assertEquals(1, emptyRow.size(), "Size should increase after add");

        // Remove a field and verify size decreases
        populatedRow.remove("field1");
        assertEquals(4, populatedRow.size(), "Size should decrease after remove");
    }

    @Test
    void testUpperCaseFieldNames() {
        // Convert field names to upper case
        populatedRow.upperCaseFieldNames();

        // Verify original field names no longer work
        Exception exception = assertThrows(RuntimeException.class, () -> {
            populatedRow.get("field1");
        }, "Should throw exception for original field name");

        // Verify upper case field names work
        assertEquals("value1", populatedRow.get("FIELD1"), "Upper case field name should work");
        assertEquals("123", populatedRow.get("FIELD2"), "Upper case field name should work");
    }
}