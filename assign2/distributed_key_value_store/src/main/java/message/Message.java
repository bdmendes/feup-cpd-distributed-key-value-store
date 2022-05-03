package message;

enum MessageType {
    PUT,
    GET,
    DELETE,
    JOIN,
    LEAVE,
    MEMBERSHIP
}

public abstract class Message {
    protected MessageType messageType;

    public Message(MessageType messageType){
        this.messageType = messageType;
    }

    protected abstract byte[] encode(String[] fields, byte[] body);
}
