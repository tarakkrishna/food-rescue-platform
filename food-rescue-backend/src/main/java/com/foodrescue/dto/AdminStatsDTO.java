package com.foodrescue.dto;

import java.time.LocalDateTime;

public class AdminStatsDTO {
    
    private long totalFoodAdded;
    private long totalFoodClaimed;
    private long totalFoodExpired;
    private long pendingClaimsCount;
    private java.util.List<TopUserDTO> topRestaurants;
    private java.util.List<TopUserDTO> topNgos;
    
    public static class TopUserDTO {
        private Long id;
        private String name;
        private long count;
        
        public TopUserDTO(Long id, String name, long count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
    
    public long getTotalFoodAdded() { return totalFoodAdded; }
    public void setTotalFoodAdded(long totalFoodAdded) { this.totalFoodAdded = totalFoodAdded; }
    
    public long getTotalFoodClaimed() { return totalFoodClaimed; }
    public void setTotalFoodClaimed(long totalFoodClaimed) { this.totalFoodClaimed = totalFoodClaimed; }
    
    public long getTotalFoodExpired() { return totalFoodExpired; }
    public void setTotalFoodExpired(long totalFoodExpired) { this.totalFoodExpired = totalFoodExpired; }
    
    public long getPendingClaimsCount() { return pendingClaimsCount; }
    public void setPendingClaimsCount(long pendingClaimsCount) { this.pendingClaimsCount = pendingClaimsCount; }
    
    public java.util.List<TopUserDTO> getTopRestaurants() { return topRestaurants; }
    public void setTopRestaurants(java.util.List<TopUserDTO> topRestaurants) { this.topRestaurants = topRestaurants; }
    
    public java.util.List<TopUserDTO> getTopNgos() { return topNgos; }
    public void setTopNgos(java.util.List<TopUserDTO> topNgos) { this.topNgos = topNgos; }
}
