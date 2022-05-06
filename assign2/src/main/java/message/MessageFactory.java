package message;


public class MessageFactory {
    private static String readLine(String data) {
        return data.substring(0, data.indexOf(0x0D));
    }

    public static Message createMessage(String headers, byte[] body) {
        String firstLine = readLine(headers);
        MessageType type = MessageType.valueOf(firstLine);

        return switch (type) {
            case PUT -> new PutMessage(headers, body);
            case GET -> new GetMessage(headers);
            case DELETE -> new DeleteMessage(headers);
            case JOIN -> new JoinMessage(headers, body);
            case LEAVE -> new LeaveMessage(headers, body);
            case MEMBERSHIP -> new MembershipMessage(headers, body);
        };
    }
}
