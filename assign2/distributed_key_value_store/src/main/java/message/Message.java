package message;

import server.MessageVisitor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class Message {
    private static final String endOfLine = new String(new byte[]{0xD, 0xA});

    protected abstract void decode(String message);

    public abstract void accept(MessageVisitor visitor);

    protected static byte[] encodeWithFields(MessageType type, Map<String, String> fields, byte[] data){
        StringBuilder builder = new StringBuilder();
        builder.append(type.toString()).append(endOfLine);
        for (Map.Entry<String,String> field: fields.entrySet()){
            builder.append(field.getKey()).append(" ").append(field.getValue()).append(endOfLine);
        }
        builder.append(endOfLine);
        builder.append(new String(data));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    protected static Map<String, String> decodeFields(String message){
        String[] lines = message.split(endOfLine);
        HashMap<String, String> fields = new HashMap<>();
        for (int i = 1; i < lines.length; i++){
            if (lines[i].isEmpty()) return fields;
            String[] content = lines[i].split(" ");
            fields.put(content[0], content[1]);
        }
        return fields;
    }

    protected static byte[] decodeBody(String message){
        int start = message.indexOf(endOfLine + endOfLine);

        if (start == -1) {
            return new byte[0];
        }

        String body = message.substring(start + endOfLine.length() * 2);
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
