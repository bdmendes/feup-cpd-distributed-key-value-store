package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRMI extends Remote {
    Status join() throws RemoteException;

    Status leave() throws RemoteException;

    enum Status {
        OK,
        ERROR,
        ALREADY_JOINED,
        JOIN_IN_PROGRESS,
        ALREADY_LEFT,
    }
}
