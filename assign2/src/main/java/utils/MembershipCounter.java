package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class MembershipCounter {
    private final AtomicInteger counter = new AtomicInteger(-1);
    private final String filePath;

    public MembershipCounter(String filePath) {
        this.filePath = filePath;
        readFromFile();
    }

    public int get() {
        return counter.get();
    }

    public void set(int newValue) {
        counter.set(newValue);
        writeToFile();
    }

    public int getAndIncrement() {
        int c = counter.getAndIncrement();
        writeToFile();
        return c;
    }

    public int incrementAndGet() {
        int c = counter.incrementAndGet();
        writeToFile();
        return c;
    }

    private void readFromFile(){
        if (filePath == null){
            return;
        }
        int c;
        try {
            Scanner scanner = new Scanner(new File(filePath));
            c = scanner.nextInt();
            counter.set(c);
            scanner.close();
        } catch (Exception e) {
            counter.set(-1);
        }
    }

    private void writeToFile(){
        if (filePath == null){
            return;
        }
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(Integer.toString(counter.get()));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
