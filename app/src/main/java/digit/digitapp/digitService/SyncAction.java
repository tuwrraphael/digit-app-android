package digit.digitapp.digitService;

import java.util.Date;

public class SyncAction {
    private String id;
    private Date deadline;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }
}
