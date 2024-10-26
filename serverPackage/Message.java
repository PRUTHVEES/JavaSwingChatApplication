package serverPackage;

import java.util.Date;

public class Message {
    private String content;
    private Date timestamp;

    public Message(String content) {
        this.content = content;
        this.timestamp = new Date(); // Store the current date and time
    }

    public String getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp.toString() + "] " + content;
    }
}
