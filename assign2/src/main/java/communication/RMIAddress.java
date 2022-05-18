package communication;

import java.util.regex.Matcher;

public class RMIAddress extends Address {
    private final String ip;
    private final String objectName;

    public RMIAddress(String ip, String objectName) {
        this.ip = ip;
        this.objectName = objectName;
    }

    public RMIAddress(String address) {
        Matcher matcher = getMatcher(address);

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

