package server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public record Node(String id, int port) {
    public NetworkInterface getNetworkInterfaceBindToIP() throws UnknownHostException, SocketException {
        InetAddress address = InetAddress.getByName(id());
        return NetworkInterface.getByInetAddress(address);
    }
}
