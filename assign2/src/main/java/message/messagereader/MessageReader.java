package message.messagereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public class MessageReader {
    private MessageReaderState state;
    protected StringBuilder header;
    protected int length;
    protected char[] body;

    public MessageReader() {
        state = new TypeReaderState(this);
        header = new StringBuilder();
    }

    protected void setState(MessageReaderState state) {
        this.state = state;
    }

    protected void resetState() {
        state = new TypeReaderState(this);
        header.setLength(0);
    }

    public void read(BufferedReader in) throws IOException {
        state.read(in);
    }

    public boolean isComplete() {
        return state.isComplete();
    }

    public String getHeader() {
        return header.toString();
    }

    public byte[] getBody() {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(body));
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        return data;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
