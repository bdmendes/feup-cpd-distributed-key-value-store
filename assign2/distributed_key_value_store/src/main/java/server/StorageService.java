package server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StorageService {
    private final int port;
    private final int id;

    public StorageService(int id, int port) throws IOException {
        this.port = port;
        this.id = id;
        Files.createDirectories(Paths.get("./node_storage/storage" + id));
    }

    public void put(String key, byte[] value) throws IOException {
        try (FileOutputStream fileWriter = new FileOutputStream(getValueFilePath(key), false)) {
            fileWriter.write(value);
        }
    }

    public byte[] get(String key) throws IOException {
        try (FileInputStream fileReader = new FileInputStream(getValueFilePath(key))) {
            return fileReader.readAllBytes();
        }
    }

    public boolean delete(String key){
        File file = new File(getValueFilePath(key));
        return file.delete();
    }

    public int getPort() {
        return this.port;
    }

    public int getId() {
        return this.id;
    }

    public String getValueFilePath(String key){
        return getValueFilesDirectory() + "/" + key;
    }

    public String getValueFilesDirectory(){
        return "./node_storage/storage" + id;
    }
}
