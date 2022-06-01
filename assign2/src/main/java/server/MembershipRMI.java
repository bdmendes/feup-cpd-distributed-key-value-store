package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRMI extends Remote {
    enum Status {
        OK,
        ERROR,
        ALREADY_JOINED,
        JOIN_IN_PROGRESS,
        ALREADY_LEFT,
        LEAVE_IN_PROGRESS,
    }


    Status join() throws RemoteException;

    Status leave() throws RemoteException;
}
