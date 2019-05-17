package digit.digitapp.digitService;

import java.util.Date;
import java.util.List;

public class LocationResponse {
    private Date nextUpdateRequiredAt;
    private List<GeofenceRequest> geofences;

    public Date getNextUpdateRequiredAt() {
        return nextUpdateRequiredAt;
    }

    public void setNextUpdateRequiredAt(Date nextUpdateRequiredAt) {
        this.nextUpdateRequiredAt = nextUpdateRequiredAt;
    }

    public List<GeofenceRequest> getGeofences() {
        return geofences;
    }

    public void setGeofences(List<GeofenceRequest> geofences) {
        this.geofences = geofences;
    }
}

