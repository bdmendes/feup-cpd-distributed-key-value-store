package server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import server.StorageService;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @AfterAll
    static void deleteTestAssets() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", 100, "a"));
        File directory = new File(storageService.getValueFilesDirectory());
        if(directory.exists()){
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Test
    void testGetPut() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", 100, "a"));
        storageService.put("key", new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, storageService.get("key"));

        storageService.put("key", new byte[]{1, 3});
        assertArrayEquals(new byte[]{1, 3}, storageService.get("key"));
    }

    @Test
    void testDelete() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", 100, "a"));
        storageService.put("key", new byte[]{1, 2, 3});
        assertTrue(new File(storageService.getValueFilePath("key")).exists());
        storageService.delete("key");
        assertFalse(new File(storageService.getValueFilePath("key")).exists());
    }
}
