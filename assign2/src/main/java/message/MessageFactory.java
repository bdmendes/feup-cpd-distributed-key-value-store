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
            case PUT_RELAY -> new PutRelayMessage(headers, body);
            case PUT_REPLY -> new PutReply(headers);
            case GET -> new GetMessage(headers);
            case GET_RELAY -> new GetRelayMessage(headers);
            case GET_REPLY -> new GetReply(headers, body);
            case DELETE -> new DeleteMessage(headers);
            case DELETE_REPLY -> new DeleteReply(headers);
            case JOIN -> new JoinMessage(headers);
            case MEMBERSHIP -> new MembershipMessage(headers, body);
            case ELECTION -> new ElectionMessage(headers, body);
            case LEADER -> new LeaderMessage(headers);
            case PUT_RELAY_REPLY -> new PutRelayReply(headers);
        };
    }
}
