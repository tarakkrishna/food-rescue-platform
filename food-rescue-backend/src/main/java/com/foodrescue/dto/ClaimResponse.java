package com.foodrescue.dto;

import com.foodrescue.entity.Claim;
import java.time.LocalDateTime;

public class ClaimResponse {
    
    private Long id;
    private Long foodItemId;
    private String foodItemName;
    private Double foodItemLatitude;
    private Double foodItemLongitude;
    private Long ngoId;
    private String ngoName;
    private LocalDateTime claimDate;
    private String status;
    private LocalDateTime approvedDate;
    private LocalDateTime pickupDate;
    private String rejectionReason;
    private Long restaurantId;
    private String restaurantName;
    private String quantity;
    
    public ClaimResponse(Claim claim) {
        this.id = claim.getId();
        if (claim.getFoodItem() != null) {
            this.foodItemId = claim.getFoodItem().getId();
            this.foodItemName = claim.getFoodItem().getName();
            this.foodItemLatitude = claim.getFoodItem().getLatitude();
            this.foodItemLongitude = claim.getFoodItem().getLongitude();
            this.quantity = claim.getFoodItem().getQuantity();
            if (claim.getFoodItem().getRestaurant() != null) {
                this.restaurantId = claim.getFoodItem().getRestaurant().getId();
                this.restaurantName = claim.getFoodItem().getRestaurant().getName();
            }
        }
        if (claim.getNgo() != null) {
            this.ngoId = claim.getNgo().getId();
            this.ngoName = claim.getNgo().getName();
        }
        this.claimDate = claim.getClaimDate();
        this.status = claim.getStatus().name();
        this.approvedDate = claim.getApprovedDate();
        this.pickupDate = claim.getPickupDate();
        this.rejectionReason = claim.getRejectionReason();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getFoodItemId() { return foodItemId; }
    public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }
    
    public String getFoodItemName() { return foodItemName; }
    public void setFoodItemName(String foodItemName) { this.foodItemName = foodItemName; }
    
    public Long getNgoId() { return ngoId; }
    public void setNgoId(Long ngoId) { this.ngoId = ngoId; }
    
    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }
    
    public LocalDateTime getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDateTime claimDate) { this.claimDate = claimDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDateTime approvedDate) { this.approvedDate = approvedDate; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public Double getFoodItemLatitude() { return foodItemLatitude; }
    public void setFoodItemLatitude(Double foodItemLatitude) { this.foodItemLatitude = foodItemLatitude; }
    
    public Double getFoodItemLongitude() { return foodItemLongitude; }
    public void setFoodItemLongitude(Double foodItemLongitude) { this.foodItemLongitude = foodItemLongitude; }
    
    public LocalDateTime getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDateTime pickupDate) { this.pickupDate = pickupDate; }
    
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    
    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
    
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
}
