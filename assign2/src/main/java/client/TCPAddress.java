package client;

import java.util.regex.Matcher;

public class TCPAddress extends IPAddress{
    private int port;
    private String ip;

    public TCPAddress(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public TCPAddress(String address) {
        Matcher matcher = getMatcher(address);

        if(matcher.find()){
            this.ip = matcher.group(1);
            try {
                this.port = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port is not a number");
            }
        }
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
