package server.messagereader;

import message.MessageType;

import java.io.BufferedReader;
import java.io.IOException;

public class TypeReaderState extends MessageReaderState {
    public TypeReaderState(MessageReader context) {
        super(context);
    }

    @Override
    public void read(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            return;
        }
        System.out.println("TypeReaderState: " + line);

        try {
            MessageType.valueOf(line);
        } catch (IllegalArgumentException e) {
            context.header.setLength(0);
            return;
        }

        addLine(line);
        context.setState(new LengthReaderState(context));
    }
}
