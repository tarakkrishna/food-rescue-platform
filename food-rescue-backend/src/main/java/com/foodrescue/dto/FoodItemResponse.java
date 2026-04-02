package com.foodrescue.dto;

import com.foodrescue.entity.FoodItem;
import java.time.LocalDate;

public class FoodItemResponse {
    
    private Long id;
    private String name;
    private String quantity;
    private LocalDate expiryDate;
    private String status;
    private Long restaurantId;
    private String restaurantName;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    
    public FoodItemResponse(FoodItem foodItem) {
        this.id = foodItem.getId();
        this.name = foodItem.getName();
        this.quantity = foodItem.getQuantity();
        this.expiryDate = foodItem.getExpiryDate();
        this.status = foodItem.getStatus().name();
        this.latitude = foodItem.getLatitude();
        this.longitude = foodItem.getLongitude();
        if (foodItem.getRestaurant() != null) {
            this.restaurantId = foodItem.getRestaurant().getId();
            this.restaurantName = foodItem.getRestaurant().getName();
        }
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    
    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
