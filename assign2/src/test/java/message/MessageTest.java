package message;

import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private static final String endOfLine = new String(new byte[]{0xD, 0xA});

    static class TestMessage extends Message {

        @Override
        public byte[] encode() {
            return new byte[0];
        }

        @Override
        public void accept(MessageVisitor visitor, Socket socket) {

        }
    }

    @Test
    void encodeWithFields() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("fruit", "beans");
        fields.put("key", "hey");
        byte[] data = new byte[]{(byte)'c', (byte)'d'};
        Message message = new TestMessage();

        byte[] result = message.encodeWithFields(MessageType.DELETE, fields, data);
        String stringResult = new String(result);

        assertEquals("DELETE" + endOfLine + "2" + endOfLine + "fruit: beans" + endOfLine + "key: hey" + endOfLine + endOfLine + "cd", stringResult);
    }

    @Test
    void decodeFields() {
        String message = "DELETE" + endOfLine + "2" + endOfLine + "fruit: beans" + endOfLine + "key: hey" + endOfLine + endOfLine + "cd";
        Message test = new TestMessage();

        var fields = test.decodeFields(message);

        assertEquals(fields.size(), 2);
        assertTrue(fields.containsKey("fruit"));
        assertEquals(fields.get("fruit"), "beans");
        assertTrue(fields.containsKey("key"));
        assertEquals(fields.get("key"), "hey");
    }
}