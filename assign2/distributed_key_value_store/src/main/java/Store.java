import java.io.IOException;

public class Store {
    public static void main(String[] args) throws IOException {
        StorageService storageService = new StorageService(1, 100);
        byte[] test = {64, 65};
        storageService.put("key1", test);
        storageService.put("key2", new byte[]{67, 67, 69});
        storageService.put("key3", test);

        System.out.println("Hello, World!");
    }
}
