package message;


public class MessageFactory {
    private static String readLine(String data) {
        return data.substring(0, data.indexOf(0xDA));
    }
    private static String consumeLine(String data) {
        return data.substring(data.indexOf(0xDA) + 2);
    }

    public static Message createMessage(byte[] message) {
        String stringMessage = new String(message);
        String firstLine = readLine(stringMessage);
        String remainingMessage = consumeLine(stringMessage);
        MessageType type = MessageType.valueOf(firstLine);

        Message messageObj = switch (type) {
            case PUT -> new PutMessage();
            case GET -> new GetMessage();
            case DELETE -> new DeleteMessage();
            case JOIN -> new JoinMessage();
            case LEAVE -> new LeaveMessage();
            case MEMBERSHIP -> new MembershipMessage();
        };
        messageObj.decode(remainingMessage);
        return messageObj;
    }
}
