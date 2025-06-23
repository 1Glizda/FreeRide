package com.cristeabogdan.freeride.database;

import android.os.AsyncTask;
import android.util.Log;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MongoDBManager {
    private static final String TAG = "MongoDBManager";
    
    // Replace with your MongoDB connection string
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "freeride";
    private static final String CARS_COLLECTION = "cars";
    
    private static MongoDBManager instance;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> carsCollection;
    private ExecutorService executorService;

    private MongoDBManager() {
        executorService = Executors.newFixedThreadPool(4);
        initializeConnection();
    }

    public static synchronized MongoDBManager getInstance() {
        if (instance == null) {
            instance = new MongoDBManager();
        }
        return instance;
    }

    private void initializeConnection() {
        executorService.execute(() -> {
            try {
                mongoClient = MongoClients.create(CONNECTION_STRING);
                database = mongoClient.getDatabase(DATABASE_NAME);
                carsCollection = database.getCollection(CARS_COLLECTION);
                
                // Create geospatial index for H3
                carsCollection.createIndex(new Document("h3Index", 1));
                carsCollection.createIndex(new Document("location", "2dsphere"));
                
                Log.d(TAG, "MongoDB connection established");
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to MongoDB", e);
            }
        });
    }

    public interface CarQueryCallback {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }

    public interface CarUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void insertCar(Car car, CarUpdateCallback callback) {
        executorService.execute(() -> {
            try {
                Document carDoc = new Document("carId", car.getCarId())
                        .append("latitude", car.getLatitude())
                        .append("longitude", car.getLongitude())
                        .append("h3Index", car.getH3Index())
                        .append("isAvailable", car.isAvailable())
                        .append("driverName", car.getDriverName())
                        .append("carModel", car.getCarModel())
                        .append("licensePlate", car.getLicensePlate())
                        .append("location", new Document("type", "Point")
                                .append("coordinates", List.of(car.getLongitude(), car.getLatitude())));

                carsCollection.insertOne(carDoc);
                
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting car", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void updateCarLocation(String carId, double latitude, double longitude, String h3Index, CarUpdateCallback callback) {
        executorService.execute(() -> {
            try {
                Bson filter = Filters.eq("carId", carId);
                Bson updates = Updates.combine(
                        Updates.set("latitude", latitude),
                        Updates.set("longitude", longitude),
                        Updates.set("h3Index", h3Index),
                        Updates.set("location", new Document("type", "Point")
                                .append("coordinates", List.of(longitude, latitude)))
                );

                carsCollection.updateOne(filter, updates);
                
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating car location", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void updateCarAvailability(String carId, boolean isAvailable, CarUpdateCallback callback) {
        executorService.execute(() -> {
            try {
                Bson filter = Filters.eq("carId", carId);
                Bson update = Updates.set("isAvailable", isAvailable);

                carsCollection.updateOne(filter, update);
                
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating car availability", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void getCarsInH3Indices(List<String> h3Indices, CarQueryCallback callback) {
        executorService.execute(() -> {
            try {
                List<Car> cars = new ArrayList<>();
                Bson filter = Filters.and(
                        Filters.in("h3Index", h3Indices),
                        Filters.eq("isAvailable", true)
                );

                for (Document doc : carsCollection.find(filter)) {
                    Car car = new Car(
                            doc.getString("carId"),
                            doc.getDouble("latitude"),
                            doc.getDouble("longitude"),
                            doc.getString("h3Index"),
                            doc.getBoolean("isAvailable"),
                            doc.getString("driverName"),
                            doc.getString("carModel"),
                            doc.getString("licensePlate")
                    );
                    cars.add(car);
                }

                if (callback != null) {
                    callback.onSuccess(cars);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying cars", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void getAllCars(CarQueryCallback callback) {
        executorService.execute(() -> {
            try {
                List<Car> cars = new ArrayList<>();
                
                for (Document doc : carsCollection.find()) {
                    Car car = new Car(
                            doc.getString("carId"),
                            doc.getDouble("latitude"),
                            doc.getDouble("longitude"),
                            doc.getString("h3Index"),
                            doc.getBoolean("isAvailable"),
                            doc.getString("driverName"),
                            doc.getString("carModel"),
                            doc.getString("licensePlate")
                    );
                    cars.add(car);
                }

                if (callback != null) {
                    callback.onSuccess(cars);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting all cars", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void getCarById(String carId, CarQueryCallback callback) {
        executorService.execute(() -> {
            try {
                Document doc = carsCollection.find(Filters.eq("carId", carId)).first();
                List<Car> cars = new ArrayList<>();
                
                if (doc != null) {
                    Car car = new Car(
                            doc.getString("carId"),
                            doc.getDouble("latitude"),
                            doc.getDouble("longitude"),
                            doc.getString("h3Index"),
                            doc.getBoolean("isAvailable"),
                            doc.getString("driverName"),
                            doc.getString("carModel"),
                            doc.getString("licensePlate")
                    );
                    cars.add(car);
                }

                if (callback != null) {
                    callback.onSuccess(cars);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting car by ID", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}