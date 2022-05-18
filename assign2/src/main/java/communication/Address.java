package communication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Address {
    Matcher getMatcher(String address) {
        Pattern pattern = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(.+)$");
        return pattern.matcher(address);
    }
}
