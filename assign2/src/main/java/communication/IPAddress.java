package communication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;

public class IPAddress extends Address {
    private int port;
    private InetAddress ip;

    public IPAddress(String ip, int port) throws UnknownHostException {
        this.ip = InetAddress.getByName(ip);
        this.port = port;
    }

    public IPAddress(String address) throws UnknownHostException {
        Matcher matcher = getMatcher(address);

        if (matcher.find()){
            this.ip = InetAddress.getByName(matcher.group(1));
            try {
                this.port = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port is not a number");
            }
        }
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
