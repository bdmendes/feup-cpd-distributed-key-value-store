package server.state;

import communication.CommunicationUtils;
import message.*;
import server.MembershipService;
import server.tasks.JoinInitTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class InitNodeState extends NodeState {
    public InitNodeState(MembershipService membershipService) {
        super(membershipService);
    }

    @Override
    public void processPut(PutMessage putMessage, Socket socket) {
        CommunicationUtils.sendErrorResponse(new PutReply(), StatusCode.NODE_NOT_JOINED, putMessage.getKey(), socket);
    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {
        CommunicationUtils.sendErrorResponse(new PutReply(), StatusCode.NODE_NOT_JOINED, getMessage.getKey(), socket);
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        CommunicationUtils.sendErrorResponse(new DeleteReply(), StatusCode.NODE_NOT_JOINED, deleteMessage.getKey(), socket);
    }

    @Override
    public boolean join() {
        synchronized (this.membershipService.joinLeaveLock) {
            if (this.membershipService.isJoined()) {
                return true; // TODO: check if truly joined or just in progress and return
            }
            this.membershipService.setNodeState(new JoiningNodeState(this.membershipService));

            ServerSocket serverSocket;
            try {
                this.membershipService.initMulticastHandler();
                serverSocket = new ServerSocket();
                serverSocket.bind(new InetSocketAddress(storageService.getNode().id(), 0));
            } catch (IOException e) {
                System.out.println("Failed to create server socket");
                this.membershipService.setNodeState(this);
                return false; // TODO: return ERROR
            }

            int counter = this.membershipService.getMembershipCounter().incrementAndGet();

            JoinMessage joinMessage = membershipService.createJoinMessage(serverSocket.getLocalPort());

            JoinInitTask joinInitTask = new JoinInitTask(this.membershipService, serverSocket, joinMessage, 2000);
            Thread joinInitThread = new Thread(joinInitTask);
            joinInitThread.start();

            this.membershipService.getClusterMap().put(storageService.getNode());
            this.membershipService.getMembershipLog().put(storageService.getNode().id(), counter);

            try {
                System.out.println("Joining cluster...");
                this.membershipService.getMulticastHandler().sendMessage(joinMessage);
            } catch (IOException e) {
                System.out.println("Failed to send join message");
                this.membershipService.setNodeState(this);
                try {
                    this.membershipService.getMulticastHandler().close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    joinInitTask.close();
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

            try {
                joinInitThread.join();
            } catch (InterruptedException e) {
                this.membershipService.setNodeState(this);
                return false;
            }

            this.membershipService.setNodeState(new JoinedNodeState(this.membershipService));
        }

        return true;
    }

    @Override
    public boolean leave() {
        return true;
    }

    @Override
    public boolean joined() {
        return false;
    }
}
