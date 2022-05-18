package server;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRMI extends Remote {
    boolean join() throws IOException;
    boolean leave() throws IOException;
}
