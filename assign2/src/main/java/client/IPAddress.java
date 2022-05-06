package client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class IPAddress {
    Matcher getMatcher(String address) {
        Pattern pattern = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(.+)$");
        return pattern.matcher(address);
    }
}
