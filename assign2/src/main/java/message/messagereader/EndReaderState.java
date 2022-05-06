package message.messagereader;

import java.io.BufferedReader;
import java.io.IOException;

public class EndReaderState extends MessageReaderState {
    public EndReaderState(MessageReader context) {
        super(context);
    }

    @Override
    public void read(BufferedReader in) throws IOException {

    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
