package digit.digitapp.digitService;

import java.util.Date;

public class LogEntry {
    public LogEntry(String id, Date occurenceTime, int code, String message, String author) {
        this.id = id;
        this.occurenceTime = occurenceTime;
        Code = code;
        this.message = message;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getOccurenceTime() {
        return occurenceTime;
    }

    public void setOccurenceTime(Date occurenceTime) {
        this.occurenceTime = occurenceTime;
    }

    public int getCode() {
        return Code;
    }

    public void setCode(int code) {
        Code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    private String id;
    private Date occurenceTime;
    private int Code;
    private String message;
    private String author;

}
