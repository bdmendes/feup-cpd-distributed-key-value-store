package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StorageService {
    private final Node node;
    private final Map<String, Object> hashLocks = Collections.synchronizedMap(new HashMap<>());

    public StorageService(Node node) throws IOException {
        this.node = node;
        Files.createDirectories(Paths.get(getValueFilesDirectory()));
        this.readHashesFromFilesDirectory();
    }

    public void put(String key, byte[] value) throws IOException {
        try (FileOutputStream fileWriter = new FileOutputStream(getValueFilePath(key), false)) {
            fileWriter.write(value);
        }
        hashLocks.put(key, new Object());
    }

    public byte[] get(String key) throws IOException {
        try (FileInputStream fileReader = new FileInputStream(getValueFilePath(key))) {
            return fileReader.readAllBytes();
        }
    }

    public boolean delete(String key) {
        hashLocks.remove(key);
        File file = new File(getValueFilePath(key));
        return file.delete();
    }

    public Node getNode() {
        return this.node;
    }

    public String getValueFilePath(String key) {
        return getValueFilesDirectory() + "/" + key;
    }

    public String getValueFilesDirectory() {
        return getStorageDirectory() + "/hash_table";
    }

    public String getStorageDirectory() {
        return "./node_storage/storage" + node;
    }

    public Set<String> getHashes() {
        return hashLocks.keySet();
    }

    public Object getHashLock(String hash) {
        return hashLocks.getOrDefault(hash, new Object());
    }

    private void readHashesFromFilesDirectory() {
        File directory = new File(this.getValueFilesDirectory());
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            hashLocks.put(file.getName(), new Object());
        }
    }
}
