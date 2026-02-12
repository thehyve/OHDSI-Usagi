package org.ohdsi.usagi.indexBuilding;

import org.junit.jupiter.api.Test;
import org.ohdsi.utilities.files.Row;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadAthenaFileTest {

    @Test
    public void testReadDomainFile() {
        final String domainTestFile = "DOMAIN.csv";
        // Get the path to the DOMAIN.csv file in the test resources
        URL resourceUrl = getClass().getClassLoader().getResource(domainTestFile);
        assertNotNull(resourceUrl, domainTestFile + " file not found in test resources");

        String filePath = new File(resourceUrl.getFile()).getAbsolutePath();

        // Create a ReadAthenaFile instance with the DOMAIN.csv file
        ReadAthenaFile readAthenaFile = new ReadAthenaFile(filePath);

        // Read all rows from the file
        List<Row> rows = new ArrayList<>();
        for (Row row : readAthenaFile) {
            rows.add(row);
        }

        // Verify that we have the expected number of rows (50 data rows + 1 header row = 51 rows, but we only get data rows)
        assertEquals(50, rows.size(), "Expected 50 data rows in DOMAIN.csv");

        // Verify the first row's content
        Row firstRow = rows.get(0);
        assertEquals("Cost", firstRow.get("domain_id"), "First row domain_id should be 'Cost'");
        assertEquals("Cost", firstRow.get("domain_name"), "First row domain_name should be 'Cost'");
        assertEquals("581456", firstRow.get("domain_concept_id"), "First row domain_concept_id should be '581456'");

        // Verify a row in the middle
        Row middleRow = rows.get(25);
        assertEquals("Device/Procedure", middleRow.get("domain_id"), "Middle row domain_id should be 'Device/Procedure'");
        assertEquals("Device/Procedure", middleRow.get("domain_name"), "Middle row domain_name should be 'Device/Procedure'");
        assertEquals("41", middleRow.get("domain_concept_id"), "Middle row domain_concept_id should be '41'");

        // Verify the last row
        Row lastRow = rows.get(rows.size() - 1);
        assertEquals("Language", lastRow.get("domain_id"), "Last row domain_id should be 'Language'");
        assertEquals("Language", lastRow.get("domain_name"), "Last row domain_name should be 'Language'");
        assertEquals("33068", lastRow.get("domain_concept_id"), "Last row domain_concept_id should be '33068'");
    }
}
