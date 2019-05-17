package digit.digitapp.digitService;

import java.util.Date;

public class GeofenceRequest {
    private Date start;
    private Date end;
    private double lat;
    private double lng;
    private double radius;
    private String id;
    private String focusItemId;
    private boolean exit;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFocusItemId() {
        return focusItemId;
    }

    public void setFocusItemId(String focusItemId) {
        this.focusItemId = focusItemId;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
}

