package message;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private static final String endOfLine = new String(new byte[]{0xD, 0xA});

    @Test
    void encodeWithFields() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("fruit", "beans");
        fields.put("key", "hey");
        byte[] data = new byte[]{(byte)'c', (byte)'d'};
        byte[] result = Message.encodeWithFields(MessageType.DELETE, fields, data);
        String stringResult = new String(result);
        System.out.println(stringResult);
        assertEquals("DELETE" + endOfLine + "fruit beans" + endOfLine + "key hey" + endOfLine + endOfLine + "cd", stringResult);
    }

    @Test
    void decodeBody() {
        String message = "DELETE" + endOfLine + "fruit beans" + endOfLine + "key hey" + endOfLine + endOfLine + "cd\ngoodstuff" + endOfLine + "yea" + endOfLine + endOfLine + "afterend";

        byte[] result = Message.decodeBody(message);
        String stringResult = new String(result);

        assertEquals("cd\ngoodstuff" + endOfLine + "yea" + endOfLine + endOfLine + "afterend", stringResult);
    }

    @Test
    void decodeFields() {
        String message = "DELETE" + endOfLine + "fruit beans" + endOfLine + "key hey" + endOfLine + endOfLine + "cd";

        var fields = Message.decodeFields(message);
        assertEquals(fields.size(), 2);
        assertTrue(fields.containsKey("fruit"));
        assertEquals(fields.get("fruit"), "beans");
        assertTrue(fields.containsKey("key"));
        assertEquals(fields.get("key"), "hey");
    }
}