package server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StorageService {
    private final Node node;

    public StorageService(Node node) throws IOException {
        this.node = node;
        Files.createDirectories(Paths.get(getValueFilesDirectory()));
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

    public Node getNode() {
        return this.node;
    }

    public String getValueFilePath(String key){
        return getValueFilesDirectory() + "/" + key;
    }

    public String getValueFilesDirectory(){
        return "./node_storage/storage" + node + "/hash_table";
    }
}
