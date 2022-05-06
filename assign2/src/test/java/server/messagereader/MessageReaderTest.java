package server.messagereader;

import message.MessageConstants;
import message.messagereader.MessageReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

class MessageReaderTest {


    @Test
    void readMessage() throws IOException {
        MessageReader reader = new MessageReader();
        String headers = "PUT" + MessageConstants.END_OF_LINE +
                "8" + MessageConstants.END_OF_LINE +
                "a: b" + MessageConstants.END_OF_LINE +
                "c: d" + MessageConstants.END_OF_LINE +
                "e: d" + MessageConstants.END_OF_LINE +
                "" + MessageConstants.END_OF_LINE;
        byte[] body = {0x00, 0x03, 0x0d, 0x0a, 0x0d, 0x0a, 65, 66};
        byte[] headerBytes = headers.getBytes();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(headerBytes);
        byteArrayOutputStream.write(body);

        byte[] message = byteArrayOutputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(message);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        while(!reader.isComplete()) {
            reader.read(bufferedReader);
        }

        String header = reader.getHeader();
        byte[] bodyBytes = reader.getBody();

        Assertions.assertEquals(headers, header);
        Assertions.assertArrayEquals(body, bodyBytes);
    }
}