import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {
    @Test
    void testGetPut() throws IOException {
        StorageService storageService = new StorageService(1, 100);
        storageService.put("key", new byte[]{1, 2, 3});
        assertEquals(new byte[]{1,2,3}, storageService.get("key"));
    }
}