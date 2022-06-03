package message;

public abstract class KeyMessage extends StatusMessage {
    private String key;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
