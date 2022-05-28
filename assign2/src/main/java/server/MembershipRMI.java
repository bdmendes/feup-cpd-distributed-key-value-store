package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRMI extends Remote {
    boolean join() throws RemoteException;

    boolean leave() throws RemoteException;
}
