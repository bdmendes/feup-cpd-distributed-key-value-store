package message;

public class MessageUtils {
    public static String readLine(String data) {
        return data.substring(0, data.indexOf(0xDA));
    }

    public static String consumeLine(String data) {
        return data.substring(data.indexOf(0xDA) + 2);
    }

}
