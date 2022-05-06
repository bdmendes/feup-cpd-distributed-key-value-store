package message;

import java.util.Map;

public abstract class ReplyMessage extends Message {
    private StatusCode statusCode = StatusCode.OK;

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    protected byte[] encodeWithFields(MessageType type, Map<String, String> fields, byte[] data) {
        fields.put("statusCode", statusCode.toString());
        return super.encodeWithFields(type, fields, data);
    }

    @Override
    protected Map<String, String> decodeFields(String message) {
        Map<String, String> fields = super.decodeFields(message);
        statusCode = StatusCode.valueOf(fields.get("statusCode"));

        return fields;
    }
}
