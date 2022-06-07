package message.messagereader;

import java.io.BufferedReader;
import java.io.IOException;

public class BodyReaderState extends MessageReaderState {
    private int numBytesRead;

    public BodyReaderState(MessageReader context) {
        super(context);
        numBytesRead = 0;
        context.body = new char[context.length];
    }

    @Override
    public void read(BufferedReader in) throws IOException {
        int read = in.read(context.body, numBytesRead, context.length - numBytesRead);
        if (read == -1) {
            return;
        }
        numBytesRead += read;

        if (numBytesRead >= context.length) {
            context.setState(new EndReaderState(context));
        }
    }
}
