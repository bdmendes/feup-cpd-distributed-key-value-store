package server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @BeforeEach
    void deleteTestAssetsBeforeEach() throws IOException {
        deleteTestAssets(new File((new StorageService(new Node("-1", 100))).getStorageDirectory()));
    }

    @AfterAll
    static void deleteTestAssetsAfterAll() throws IOException {
        deleteTestAssets(new File((new StorageService(new Node("-1", 100))).getStorageDirectory()));
    }

    static void deleteTestAssets(File directory) {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteTestAssets(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
        directory.getParentFile().delete();
    }

    @Test
    void testGetPut() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", 100));
        storageService.put("key", new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, storageService.get("key"));

        storageService.put("key", new byte[]{1, 3});
        assertArrayEquals(new byte[]{1, 3}, storageService.get("key"));
    }

    @Test
    void testDelete() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", 100));
        storageService.put("key", new byte[]{1, 2, 3});
        assertTrue(new File(storageService.getValueFilePath("key")).exists());
        storageService.delete("key", true);
        assertFalse(new File(storageService.getValueFilePath("key")).exists());
    }
}
