package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class StorageService {
    private final Node node;
    private final Map<String, Object> hashLocks = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> tombstones = Collections.synchronizedSet(new HashSet<>());
    public static String STORAGE_ROOT_PATH = "./node_storage";

    public StorageService(Node node) throws IOException {
        this.node = node;
        Files.createDirectories(Paths.get(getValueFilesDirectory()));
        Files.createDirectories(Paths.get(getTombstonesDirectory()));
        this.readHashesFromFilesDirectory();
        this.readTombstonesFromTombstonesDirectory();
    }

    public void put(String key, byte[] value) throws IOException {
        try (FileOutputStream fileWriter = new FileOutputStream(getValueFilePath(key), false)) {
            fileWriter.write(value);
        }
        hashLocks.put(key, new Object());
        if (tombstones.contains(key)) {
            tombstones.remove(key);
            File tombstone = new File(getTombstoneFilePath(key));
            if (!tombstone.delete()) {
                throw new IOException();
            }
        }
    }

    public byte[] get(String key) throws IOException {
        try (FileInputStream fileReader = new FileInputStream(getValueFilePath(key))) {
            return fileReader.readAllBytes();
        }
    }

    public boolean delete(String key, boolean tombstone) {
        hashLocks.remove(key);
        File file = new File(getValueFilePath(key));
        boolean deletedFile = file.delete();
        if (tombstone && deletedFile) {
            try (FileOutputStream fileWriter = new FileOutputStream(getTombstoneFilePath(key), false)) {
                fileWriter.write(new byte[0]);
            } catch (IOException e) {
                deletedFile = false;
            }
            tombstones.add(key);
        }
        return deletedFile;
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

    public String getTombstonesDirectory() {
        return getStorageDirectory() + "/tombstones";
    }

    public String getTombstoneFilePath(String key) {
        return getTombstonesDirectory() + "/" + key;
    }

    public String getStorageDirectory() {
        return STORAGE_ROOT_PATH + "/storage" + node;
    }

    public Set<String> getHashes() {
        return hashLocks.keySet();
    }

    public Set<String> getTombstones() {
        return tombstones;
    }

    public Object getHashLock(String hash) {
        return hashLocks.getOrDefault(hash, new Object());
    }

    public synchronized void deleteTombstones() {
        tombstones.clear();
        File directory = new File(getTombstonesDirectory());
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.delete()) {
                return;
            }
        }
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

    private void readTombstonesFromTombstonesDirectory() {
        File directory = new File(this.getTombstonesDirectory());
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            tombstones.add(file.getName());
        }
    }
}
