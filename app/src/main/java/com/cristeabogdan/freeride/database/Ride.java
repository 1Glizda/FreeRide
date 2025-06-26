package com.cristeabogdan.freeride.database;

public class Ride {
    private String id;
    private String userId;
    private String carId;
    private String status;
    private double pickupLatitude;
    private double pickupLongitude;
    private double dropLatitude;
    private double dropLongitude;
    private Double amount;
    private String distance;
    private String duration;
    private Long rating;
    private Long createdAt;
    private Long requestedAt;
    private Long driverAssignedAt;
    private Long driverArrivingAt;
    private Long driverArrivedAt;
    private Long tripStartedAt;
    private Long tripCompletedAt;
    private String feedbackId;

    public Ride() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public double getDropLatitude() {
        return dropLatitude;
    }

    public void setDropLatitude(double dropLatitude) {
        this.dropLatitude = dropLatitude;
    }

    public double getDropLongitude() {
        return dropLongitude;
    }

    public void setDropLongitude(double dropLongitude) {
        this.dropLongitude = dropLongitude;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Long getRating() {
        return rating;
    }

    public void setRating(Long rating) {
        this.rating = rating;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Long requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Long getDriverAssignedAt() {
        return driverAssignedAt;
    }

    public void setDriverAssignedAt(Long driverAssignedAt) {
        this.driverAssignedAt = driverAssignedAt;
    }

    public Long getDriverArrivingAt() {
        return driverArrivingAt;
    }

    public void setDriverArrivingAt(Long driverArrivingAt) {
        this.driverArrivingAt = driverArrivingAt;
    }

    public Long getDriverArrivedAt() {
        return driverArrivedAt;
    }

    public void setDriverArrivedAt(Long driverArrivedAt) {
        this.driverArrivedAt = driverArrivedAt;
    }

    public Long getTripStartedAt() {
        return tripStartedAt;
    }

    public void setTripStartedAt(Long tripStartedAt) {
        this.tripStartedAt = tripStartedAt;
    }

    public Long getTripCompletedAt() {
        return tripCompletedAt;
    }

    public void setTripCompletedAt(Long tripCompletedAt) {
        this.tripCompletedAt = tripCompletedAt;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    @Override
    public String toString() {
        return "Ride{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", carId='" + carId + '\'' +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", distance='" + distance + '\'' +
                ", duration='" + duration + '\'' +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                '}';
    }
}