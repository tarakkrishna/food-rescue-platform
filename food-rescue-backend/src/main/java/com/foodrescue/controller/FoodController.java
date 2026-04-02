package com.foodrescue.controller;

import com.foodrescue.dto.*;
import com.foodrescue.entity.Claim;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.repository.ClaimRepository;
import com.foodrescue.repository.FoodItemRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/food")
@CrossOrigin(origins = "*")
public class FoodController {
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private ClaimRepository claimRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LocationService locationService;
    
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableFood(Authentication authentication) {
        // Temporarily bypass authentication for testing
        if (authentication == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Please login to see available food"));
        }
        
        // Get current NGO user with their location
        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // If NGO has no location set, return error
        if (ngo.getLatitude() == null || ngo.getLongitude() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Please set your location first to see nearby donations"));
        }
        
        // Get all POSTED food items with location, filter by 30km radius
        List<FoodItem> allFoodItems = foodItemRepository.findByStatus(FoodItem.Status.POSTED);
        List<FoodItemResponse> response = allFoodItems.stream()
                .filter(item -> locationService.isWithinRadius(
                    ngo.getLatitude(), ngo.getLongitude(), item, 30.0))
                .map(item -> {
                    FoodItemResponse dto = new FoodItemResponse(item);
                    // Add distance info
                    Double distance = locationService.getDistance(
                        ngo.getLatitude(), ngo.getLongitude(), item);
                    dto.setDistanceKm(distance);
                    return dto;
                })
                .sorted((f1, f2) -> {
                    if (f1.getDistanceKm() == null) return 1;
                    if (f2.getDistanceKm() == null) return -1;
                    return Double.compare(f1.getDistanceKm(), f2.getDistanceKm());
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllFoodItems(Authentication authentication) {
        // Get all food items regardless of status
        List<FoodItem> allFoodItems = foodItemRepository.findAll();
        List<FoodItemResponse> response = allFoodItems.stream()
                .map(item -> {
                    FoodItemResponse dto = new FoodItemResponse(item);
                    // Add distance info if admin has location (optional)
                    return dto;
                })
                .sorted((f1, f2) -> f2.getId().compareTo(f1.getId())) // Sort by newest first
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/claim/{id}")
    public ResponseEntity<?> claimFood(@PathVariable Long id, Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Block untrusted NGOs from claiming
        if (!ngo.isTrusted()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Your account is pending admin approval. Please wait for approval before claiming food items."));
        }
        
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food item not found"));
        
        if (foodItem.getStatus() != FoodItem.Status.POSTED) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Food item is not available"));
        }
        
        // Check if NGO is within 30km radius
        if (ngo.getLatitude() == null || ngo.getLongitude() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Please set your location first"));
        }
        
        if (!locationService.isWithinRadius(ngo.getLatitude(), ngo.getLongitude(), foodItem, 30.0)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: This food item is outside your 30km service radius"));
        }
        
        if (claimRepository.findByFoodItemAndNgo(foodItem, ngo).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: You have already claimed this food item"));
        }
        
        // Create PENDING claim (food not yet picked up)
        Claim claim = new Claim();
        claim.setFoodItem(foodItem);
        claim.setNgo(ngo);
        claim.setClaimDate(LocalDateTime.now());
        claim.setStatus(Claim.Status.PENDING); // Changed from APPROVED to PENDING
        
        // Mark food as CLAIMED to hide it from others
        foodItem.setStatus(FoodItem.Status.CLAIMED);
        foodItemRepository.save(foodItem);
        claimRepository.save(claim);
        
        return ResponseEntity.ok(new MessageResponse("Food item reserved successfully! Please pickup within 2 hours. Route map available in your dashboard."));
    }
    
    @PostMapping("/pickup/{claimId}")
    public ResponseEntity<?> pickupFood(@PathVariable Long claimId, Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        
        // Verify this NGO owns the claim
        if (!claim.getNgo().getId().equals(ngo.getId())) {
            return ResponseEntity.status(403).body(new MessageResponse("Error: Not authorized"));
        }
        
        // Verify claim is in PENDING status
        if (claim.getStatus() != Claim.Status.PENDING) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: This food has already been picked up or cancelled"));
        }
        
        FoodItem foodItem = claim.getFoodItem();
        
        // Mark claim as picked up
        claim.setStatus(Claim.Status.PICKED_UP);
        claim.setPickupDate(LocalDateTime.now());
        
        // Mark food as CLAIMED
        foodItem.setStatus(FoodItem.Status.CLAIMED);
        foodItemRepository.save(foodItem);
        claimRepository.save(claim);
        
        return ResponseEntity.ok(new MessageResponse("Food picked up successfully! Please deliver to your center."));
    }
    
    @PostMapping("/request-transport/{claimId}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> requestTransport(@PathVariable Long claimId, Authentication authentication) {
        try {
            User ngo = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Claim claim = claimRepository.findById(claimId)
                    .orElseThrow(() -> new RuntimeException("Claim not found"));
            
            // Verify this NGO owns the claim
            if (!claim.getNgo().getId().equals(ngo.getId())) {
                return ResponseEntity.status(403).body(new MessageResponse("Error: Not authorized"));
            }
            
            // Verify claim is in PENDING status
            if (claim.getStatus() != Claim.Status.PENDING) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Transport can only be requested for pending pickups"));
            }
            
            // Mark claim as ready for volunteer pickup
            // This makes it visible in volunteer nearby pickups
            // No status change needed - PENDING claims are already visible to volunteers
            
            return ResponseEntity.ok(new MessageResponse("Transport request sent! Nearby volunteers will be notified."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/route/{foodItemId}")
    public ResponseEntity<?> getRouteToPickup(@PathVariable Long foodItemId, Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new RuntimeException("Food item not found"));
        
        if (ngo.getLatitude() == null || ngo.getLongitude() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Please set your location first"));
        }
        
        if (foodItem.getLatitude() == null || foodItem.getLongitude() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Food item has no pickup location set"));
        }
        
        // Build Google Maps directions URL
        String ngoLocation = ngo.getLatitude() + "," + ngo.getLongitude();
        String pickupLocation = foodItem.getLatitude() + "," + foodItem.getLongitude();
        String googleMapsUrl = String.format(
            "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s&travelmode=driving",
            ngoLocation, pickupLocation
        );
        
        // Alternative: OpenStreetMap URL (no API key needed)
        String osmUrl = String.format(
            "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=%s;%s",
            ngoLocation, pickupLocation
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("foodItemId", foodItem.getId());
        response.put("foodName", foodItem.getName());
        response.put("restaurantName", foodItem.getRestaurant().getName());
        response.put("ngoLocation", Map.of("lat", ngo.getLatitude(), "lng", ngo.getLongitude()));
        response.put("pickupLocation", Map.of("lat", foodItem.getLatitude(), "lng", foodItem.getLongitude()));
        response.put("distanceKm", locationService.getDistance(ngo.getLatitude(), ngo.getLongitude(), foodItem));
        response.put("googleMapsUrl", googleMapsUrl);
        response.put("openStreetMapUrl", osmUrl);
        response.put("estimatedTimeMinutes", (int)(locationService.getDistance(ngo.getLatitude(), ngo.getLongitude(), foodItem) * 2)); // Rough estimate: 2 min per km
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyDonations(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        
        List<FoodItem> unassignedDonations = foodItemRepository.findUnassignedDonationsWithLocation();
        
        List<Map<String, Object>> nearbyDonations = unassignedDonations.stream()
                .filter(d -> locationService.isWithinRadius(lat, lon, d, radiusKm))
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getName());
                    map.put("quantity", d.getQuantity());
                    map.put("expiryDate", d.getExpiryDate());
                    map.put("status", d.getStatus());
                    map.put("restaurantName", d.getRestaurant() != null ? d.getRestaurant().getName() : "Unknown");
                    map.put("latitude", d.getLatitude());
                    map.put("longitude", d.getLongitude());
                    map.put("distanceKm", locationService.getDistance(lat, lon, d));
                    return map;
                })
                .sorted((d1, d2) -> Double.compare((Double) d1.get("distanceKm"), (Double) d2.get("distanceKm")))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(nearbyDonations);
    }
}
