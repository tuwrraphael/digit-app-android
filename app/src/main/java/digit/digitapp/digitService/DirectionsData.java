package digit.digitapp.digitService;

import java.util.Date;
import java.util.List;

public class  DirectionsData {
    private Date departureTime;
    private Date arrivalTime;
    private List<LegData> legs;

    public Date getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Date departureTime) {
        this.departureTime = departureTime;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Date arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public List<LegData> getLegs() {
        return legs;
    }

    public void setLegs(List<LegData> legs) {
        this.legs = legs;
    }
}

