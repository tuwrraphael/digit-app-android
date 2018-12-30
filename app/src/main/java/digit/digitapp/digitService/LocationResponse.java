package digit.digitapp.digitService;

import java.util.Date;

public class LocationResponse {
    private Date nextUpdateRequiredAt;

    public Date getNextUpdateRequiredAt() {
        return nextUpdateRequiredAt;
    }

    public void setNextUpdateRequiredAt(Date nextUpdateRequiredAt) {
        this.nextUpdateRequiredAt = nextUpdateRequiredAt;
    }
}
