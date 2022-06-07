package message.messagereader;

import java.io.BufferedReader;

public class EndReaderState extends MessageReaderState {
    public EndReaderState(MessageReader context) {
        super(context);
    }

    @Override
    public void read(BufferedReader in) {

    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
