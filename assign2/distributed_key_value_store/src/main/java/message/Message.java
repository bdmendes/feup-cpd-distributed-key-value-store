package message;
public abstract class Message {
    protected abstract byte[] encode(String[] fields, byte[] body);
    protected abstract void decode(String message);
}
