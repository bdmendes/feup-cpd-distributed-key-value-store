package message;


public class MessageFactory {
    private static String readLine(String data) {
        return data.substring(0, data.indexOf(0xDA));
    }

    public static Message createMessage(byte[] message) {
        String stringMessage = new String(message);
        String firstLine = readLine(stringMessage);
        MessageType type = MessageType.valueOf(firstLine);

        return switch (type) {
            case PUT -> new PutMessage(stringMessage);
            case GET -> new GetMessage(stringMessage);
            case DELETE -> new DeleteMessage(stringMessage);
            case JOIN -> new JoinMessage(stringMessage);
            case LEAVE -> new LeaveMessage(stringMessage);
            case MEMBERSHIP -> new MembershipMessage(stringMessage);
        };
    }
}
