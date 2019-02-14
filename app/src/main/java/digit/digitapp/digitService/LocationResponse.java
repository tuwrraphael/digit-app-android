package digit.digitapp.digitService;

import java.util.Date;

public class LocationResponse {
    private Date nextUpdateRequiredAt;
    private GeofenceRequest requestGeofence;

    public Date getNextUpdateRequiredAt() {
        return nextUpdateRequiredAt;
    }

    public void setNextUpdateRequiredAt(Date nextUpdateRequiredAt) {
        this.nextUpdateRequiredAt = nextUpdateRequiredAt;
    }

    public GeofenceRequest getRequestGeofence() {
        return requestGeofence;
    }

    public void setRequestGeofence(GeofenceRequest requestGeofence) {
        this.requestGeofence = requestGeofence;
    }
}

