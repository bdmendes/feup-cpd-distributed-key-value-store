package message;

public abstract class ReplyKeyMessage extends ReplyMessage {
    private String key;
    public void setKey(String key){
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
