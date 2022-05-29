package message;

import message.messagereader.MessageReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static message.MessageConstants.END_OF_LINE;
import static org.junit.jupiter.api.Assertions.*;

class PutRelayMessageTest {
    @Test
    void encode() throws IOException {
        PutRelayMessage message = new PutRelayMessage();

        message.addValue("key", "value".getBytes(StandardCharsets.UTF_8));
        message.addValue("key2", "value2".getBytes(StandardCharsets.UTF_8));
        message.addValue("key3", ("value3" + END_OF_LINE).getBytes(StandardCharsets.UTF_8));
        message.addValue("key4", ("value3" + END_OF_LINE + "TEST" + END_OF_LINE + "GOOD").getBytes(StandardCharsets.UTF_8));

        byte[] encoded = message.encode();

        BufferedReader reader = new BufferedReader(new StringReader(new String(encoded)));
        MessageReader messageReader = new MessageReader();

        while(!messageReader.isComplete()) {
            messageReader.read(reader);
        }

        PutRelayMessage decoded = (PutRelayMessage) MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());

        decoded.getValues().forEach((key, value) -> {
            Assertions.assertArrayEquals(message.getValues().get(key), value);
        });
    }
}