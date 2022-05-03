package message;


public class MessageFactory {
    public static Message createMessage(byte[] message) {
        String stringMessage = new String(message);
        String firstLine = MessageUtils.readLine(stringMessage);
        String remainingMessage = MessageUtils.consumeLine(stringMessage);
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
