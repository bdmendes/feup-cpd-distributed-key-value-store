package message.messagereader;

import java.io.BufferedReader;
import java.io.IOException;

public class HeaderReaderState extends MessageReaderState {

    public HeaderReaderState(MessageReader context) {
        super(context);
    }

    @Override
    public void read(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            return;
        }

        addLine(line);
        if(line.equals("")) {
            context.setState(new BodyReaderState(context));
        }
    }
}
