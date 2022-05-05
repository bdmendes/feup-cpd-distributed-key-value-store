package server.messagereader;

import java.io.BufferedReader;
import java.io.IOException;

public class LengthReaderState extends MessageReaderState {
    public LengthReaderState(MessageReader context) {
        super(context);
    }

    @Override
    public void read(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            return;
        }

        int length;
        try {
            length = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            context.resetState();
            return;
        }

        context.setLength(length);
        addLine(line);
        context.setState(new HeaderReaderState(context));
    }
}
