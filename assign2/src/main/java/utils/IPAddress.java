package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPAddress {
    private final String ip;
    private final String objectName;

    public IPAddress(String ip, String objectName) {
        this.ip = ip;
        this.objectName = objectName;
    }

    public IPAddress(String address) {
        Pattern pattern = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(.+)$");
        Matcher matcher = pattern.matcher(address);

        if(matcher.find()) {
            this.objectName = matcher.group(2);
            this.ip = matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid IP address");
        }
    }

    public String getIp() {
        return ip;
    }

    public String getObjectName() {
        return objectName;
    }
}

