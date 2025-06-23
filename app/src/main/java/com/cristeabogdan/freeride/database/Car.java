package com.cristeabogdan.freeride.database;

public class Car {
    private String carId;
    private double latitude;
    private double longitude;
    private String h3Index;
    private boolean isAvailable;
    private String driverName;
    private String carModel;
    private String licensePlate;

    public Car() {}

    public Car(String carId, double latitude, double longitude, String h3Index, 
               boolean isAvailable, String driverName, String carModel, String licensePlate) {
        this.carId = carId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.h3Index = h3Index;
        this.isAvailable = isAvailable;
        this.driverName = driverName;
        this.carModel = carModel;
        this.licensePlate = licensePlate;
    }

    // Getters and Setters
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getH3Index() {
        return h3Index;
    }

    public void setH3Index(String h3Index) {
        this.h3Index = h3Index;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    @Override
    public String toString() {
        return "Car{" +
                "carId='" + carId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", h3Index='" + h3Index + '\'' +
                ", isAvailable=" + isAvailable +
                ", driverName='" + driverName + '\'' +
                ", carModel='" + carModel + '\'' +
                ", licensePlate='" + licensePlate + '\'' +
                '}';
    }
}