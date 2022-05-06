package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreUtilsTest {
    @Test
    void sha256Test() {
        String test = "test";
        byte[] testBytes = test.getBytes();

        String testHash = StoreUtils.sha256(testBytes);
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", testHash);
    }
}