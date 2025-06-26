package com.cristeabogdan.freeride.history;

public class RideHistoryItem {
    private String rideId;
    private Double distance;
    private Double duration;
    private Double fare;          // ← new
    private Long   timestamp;

    // getters...
    public String getRideId()    { return rideId; }
    public Double getDistance()  { return distance; }
    public Double getDuration()  { return duration; }
    public Double getFare()      { return fare; }
    public Long   getTimestamp() { return timestamp; }

    // setters...
    public void setRideId(String rideId)    { this.rideId = rideId; }
    public void setDistance(Double d)       { this.distance = d; }
    public void setDuration(Double d)       { this.duration = d; }
    public void setFare(Double f)           { this.fare = f; }  // ← new
    public void setTimestamp(Long ts)       { this.timestamp = ts; }
}
