package server.state;

import communication.CommunicationUtils;
import message.*;
import server.MembershipRMI;
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
        CommunicationUtils.sendErrorResponse(new GetReply(), StatusCode.NODE_NOT_JOINED, getMessage.getKey(), socket);
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        CommunicationUtils.sendErrorResponse(new DeleteReply(), StatusCode.NODE_NOT_JOINED, deleteMessage.getKey(), socket);
    }

    @Override
    public MembershipRMI.Status join() {
        synchronized (this.membershipService.joinLeaveLock) {
            if (this.membershipService.isJoined()) {
                if(this.membershipService.getNodeState().joined()) {
                    return MembershipRMI.Status.ALREADY_JOINED;
                }

                return MembershipRMI.Status.JOIN_IN_PROGRESS;
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
                return MembershipRMI.Status.ERROR;
            }

            int counter = this.membershipService.getMembershipCounter().get();
            counter++;

            JoinMessage joinMessage = membershipService.createJoinMessage(serverSocket.getLocalPort(), counter);

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
                return MembershipRMI.Status.ERROR;
            }

            try {
                joinInitThread.join();
            } catch (InterruptedException e) {
                this.membershipService.setNodeState(this);

                return MembershipRMI.Status.ERROR;
            }

            this.membershipService.getMembershipCounter().incrementAndGet();
            this.membershipService.setNodeState(new JoinedNodeState(this.membershipService));
        }

        return MembershipRMI.Status.OK;
    }

    @Override
    public MembershipRMI.Status leave() {
        return MembershipRMI.Status.ALREADY_LEFT;
    }

    @Override
    public boolean joined() {
        return false;
    }
}
