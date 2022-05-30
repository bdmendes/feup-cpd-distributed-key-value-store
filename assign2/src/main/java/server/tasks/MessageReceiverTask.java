package server.tasks;

import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;
import server.MembershipService;
import server.MessageProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MessageReceiverTask implements Runnable {
    private final ServerSocket serverSocket;
    private final MembershipService membershipService;
    private ExecutorService executorService;
    private final boolean running;

    public MessageReceiverTask(MembershipService membershipService, ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.membershipService = membershipService;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
        running = true;
    }

    public synchronized void waitAndRestart() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                MessageReader messageReader = new MessageReader();

                while (!messageReader.isComplete()) {
                    messageReader.read(in);
                }

                Message message = MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
                MessageProcessor processor = new MessageProcessor(membershipService, message, clientSocket);
                synchronized (this) {
                    executorService.execute(processor);
                }
            } catch (IOException e) {

                e.printStackTrace();
                break;
            }
        }
    }
}
