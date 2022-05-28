package server.state;

import communication.JoinInitMembership;
import message.JoinMessage;
import server.MembershipService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class InitNodeState extends NotInClusterState {
    public InitNodeState(MembershipService membershipService) {
        super(membershipService);
    }

    @Override
    public boolean join() {
        synchronized (this.membershipService.joinLeaveLock) {
            if(this.membershipService.isJoined()) {
                return true;
            }

            ServerSocket serverSocket;
            try {
                this.membershipService.initMulticastHandler();
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(storageService.getNode().id(), 0));
            } catch (IOException e) {
                System.out.println("Failed to create server socket");
                return false;
            }

            int counter = this.membershipService.getMembershipCounter().incrementAndGet();

            JoinMessage joinMessage = membershipService.createJoinMessage(serverSocket.getLocalPort());

            JoinInitMembership messageReceiver = new JoinInitMembership(this.membershipService, serverSocket, joinMessage, 2000);
            Thread messageReceiverThread = new Thread(messageReceiver);
            messageReceiverThread.start();

            this.membershipService.getClusterMap().put(storageService.getNode());
            this.membershipService.getMembershipLog().put(storageService.getNode().id(), counter);

            try {
                System.out.println("Joining cluster...");
                this.membershipService.getMulticastHandler().sendMessage(joinMessage);
            } catch (IOException e) {
                System.out.println("Failed to send join message");
                try {
                    this.membershipService.getMulticastHandler().close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    messageReceiver.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }

            //Thread multicastHandlerThread = new Thread(this.membershipService.getMulticastHandler());
            //multicastHandlerThread.start();
        }

        return true;
    }
}
