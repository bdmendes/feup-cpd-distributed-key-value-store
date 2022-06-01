package message;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static message.MessageConstants.END_OF_LINE;

public class PutRelayMessage extends Message {
    public static final int MAX_MESSAGES = 100;
    private int numValues;
    private Map<String, byte[]> values;
    private boolean transference = false;

    public PutRelayMessage() {
        values = new LinkedHashMap<>();
    }

    public PutRelayMessage(String headers, byte[] data) {
        Map<String, String> fields = decodeFields(headers);
        numValues = Integer.parseInt(fields.get("numValues"));
        transference = Boolean.parseBoolean(fields.get("transference"));

        parseValues(data);
    }

    public void setTransference(boolean transference) {
        this.transference = transference;
    }

    public boolean isTransference() {
        return this.transference;
    }

    /**
     * Finds the 0x0D 0x0A delimiter.
     *
     * @param data  The data to parse.
     * @param start The start index.
     * @return the index of the delimiter.
     */
    private int findEndOfLine(byte[] data, int start) {
        int end = start;
        int count = 0;
        while (end < data.length) {
            switch (count) {
                case 0:
                    if (data[end] == 0x0D) {
                        count++;
                    }
                    break;
                case 1:
                    if (data[end] == 0x0A) {
                        return end - 1;
                    } else if (data[end] != 0x0D) {
                        count = 0;
                    }
                    break;
            }

            end++;
        }
        return end;
    }

    private void parseValues(byte[] data) {
        this.values = new HashMap<>();
        Map<String, Integer> lengthOfMessage = new LinkedHashMap<>(numValues);
        int start = 0;

        for (int i = 0; i < numValues; i++) {
            int end = findEndOfLine(data, start);
            String line = new String(data, start, end - start);

            String[] parts = line.split(" ");
            String key = parts[0];
            int length = Integer.parseInt(parts[1]);

            lengthOfMessage.put(key, length);
            start = end + 2;
        }

        for (Map.Entry<String, Integer> entry : lengthOfMessage.entrySet()) {
            String key = entry.getKey();
            int length = entry.getValue();
            byte[] value = Arrays.copyOfRange(data, start, start + length);
            values.put(key, value);
            start += length;
        }
    }

    public Map<String, byte[]> getValues() {
        return values;
    }

    public boolean addValue(String key, byte[] value) {
        if (values.size() >= MAX_MESSAGES) {
            return true;
        }

        values.put(key, value);
        return values.size() >= MAX_MESSAGES;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("numValues", String.valueOf(values.size()));
        fields.put("transference", Boolean.valueOf(transference).toString());

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, byte[]> entry : values.entrySet()) {
            builder.append(entry.getKey()).append(" ").append(entry.getValue().length).append(END_OF_LINE);
        }

        for (Map.Entry<String, byte[]> entry : values.entrySet()) {
            builder.append(new String(entry.getValue()));
        }

        return encodeWithFields(MessageType.PUT_RELAY, fields, builder.toString().getBytes());
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processPutRelay(this, socket);
    }

}
