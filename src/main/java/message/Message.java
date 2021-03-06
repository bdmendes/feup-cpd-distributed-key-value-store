package message;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static message.MessageConstants.END_OF_LINE;

public abstract class Message {
    protected byte[] encodeWithFields(MessageType type, Map<String, String> fields, byte[] data) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString()).append(END_OF_LINE);
        builder.append(data.length).append(END_OF_LINE);

        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (field.getValue() == null) continue;

            builder.append(field.getKey()).append(": ").append(field.getValue()).append(END_OF_LINE);
        }
        builder.append(END_OF_LINE);
        builder.append(new String(data));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    protected Map<String, String> decodeFields(String message) {
        String[] lines = message.split(END_OF_LINE);
        HashMap<String, String> fields = new HashMap<>();
        for (int i = 2; i < lines.length; i++) {
            if (lines[i].isEmpty()) return fields;
            String[] content = lines[i].split(": ");
            String value = content.length > 1 ? content[1] : "";
            fields.put(content[0], value);
        }

        return fields;
    }

    public abstract byte[] encode();

    public abstract void accept(MessageVisitor visitor, Socket socket);
}
