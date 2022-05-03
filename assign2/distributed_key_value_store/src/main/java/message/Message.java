package message;

import server.MessageVisitor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static message.MessageConstants.END_OF_LINE;

public abstract class Message {

    protected static byte[] encodeWithFields(MessageType type, Map<String, String> fields, byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString()).append(END_OF_LINE);
        for (Map.Entry<String, String> field : fields.entrySet()) {
            builder.append(field.getKey()).append(" ").append(field.getValue()).append(END_OF_LINE);
        }
        builder.append(END_OF_LINE);
        builder.append(new String(data));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    protected static Map<String, String> decodeFields(String message) {
        String[] lines = message.split(END_OF_LINE);
        HashMap<String, String> fields = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) return fields;
            String[] content = lines[i].split(" ");
            fields.put(content[0], content[1]);
        }
        return fields;
    }

    protected static byte[] decodeBody(String message) {
        int start = message.indexOf(END_OF_LINE + END_OF_LINE);

        if (start == -1) {
            return new byte[0];
        }

        String body = message.substring(start + END_OF_LINE.length() * 2);
        return body.getBytes(StandardCharsets.UTF_8);
    }

    public abstract byte[] encode();

    public abstract void accept(MessageVisitor visitor);
}
