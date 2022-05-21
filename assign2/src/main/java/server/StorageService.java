package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StorageService {
    private final Node node;
    private final Set<String> hashes = Collections.synchronizedSet(new HashSet<>());

    public StorageService(Node node) throws IOException {
        this.node = node;
        Files.createDirectories(Paths.get(getValueFilesDirectory()));
        this.readHashesFromFilesDirectory();
    }

    public void put(String key, byte[] value) throws IOException {
        try (FileOutputStream fileWriter = new FileOutputStream(getValueFilePath(key), false)) {
            fileWriter.write(value);
        }
        hashes.add(key);
    }

    public byte[] get(String key) throws IOException {
        try (FileInputStream fileReader = new FileInputStream(getValueFilePath(key))) {
            return fileReader.readAllBytes();
        }
    }

    public boolean delete(String key) {
        hashes.remove(key);
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
        return hashes;
    }

    private void readHashesFromFilesDirectory() {
        File directory = new File(this.getValueFilesDirectory());
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            hashes.add(file.getName());
        }
    }
}
