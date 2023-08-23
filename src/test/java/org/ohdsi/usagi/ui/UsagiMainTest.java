package org.ohdsi.usagi.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsagiMainTest {

    // verifies that the version from pom.xml is being picked up properly
    @Test
    public void testGetVersion() {
        assertNotEquals(UsagiMain.NO_VERSION, UsagiMain.getVersion());
    }
}