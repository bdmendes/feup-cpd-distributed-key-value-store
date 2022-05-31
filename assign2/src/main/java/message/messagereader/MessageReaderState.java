package message.messagereader;

import message.MessageConstants;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class MessageReaderState {
    protected final MessageReader context;

    public MessageReaderState(MessageReader context) {
        this.context = context;
    }

    public abstract void read(BufferedReader in) throws IOException;

    protected void addLine(String line) {
        context.header.append(line).append(MessageConstants.END_OF_LINE);
    }

    public boolean isComplete() {
        return false;
    }
}
