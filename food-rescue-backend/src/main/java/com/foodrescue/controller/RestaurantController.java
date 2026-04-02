package com.foodrescue.controller;

import com.foodrescue.dto.*;
import com.foodrescue.entity.Claim;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.repository.ClaimRepository;
import com.foodrescue.repository.FoodItemRepository;
import com.foodrescue.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/restaurant")
@CrossOrigin(origins = "http://localhost:3000")
public class RestaurantController {
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private ClaimRepository claimRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/add-food")
    public ResponseEntity<?> addFood(@Valid @RequestBody FoodItemRequest request, 
                                     Authentication authentication) {
        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        FoodItem foodItem = new FoodItem();
        foodItem.setRestaurant(restaurant);
        foodItem.setName(request.getName());
        foodItem.setQuantity(request.getQuantity());
        foodItem.setExpiryDate(request.getExpiryDate());
        foodItem.setStatus(FoodItem.Status.POSTED);
        
        // Validate and set location
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Latitude and longitude are required for donation"));
        }
        foodItem.setLatitude(request.getLatitude());
        foodItem.setLongitude(request.getLongitude());
        
        foodItemRepository.save(foodItem);
        
        return ResponseEntity.ok(new MessageResponse("Food item added successfully!"));
    }
    
    @GetMapping("/food-list")
    public ResponseEntity<?> getFoodList(Authentication authentication) {
        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<FoodItem> foodItems = foodItemRepository.findByRestaurant(restaurant);
        List<FoodItemResponse> response = foodItems.stream()
                .map(FoodItemResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pending-claims")
    public ResponseEntity<?> getPendingClaims(Authentication authentication) {
        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Claim> pendingClaims = claimRepository.findByFoodItemRestaurantAndStatus(restaurant, Claim.Status.PENDING);
        List<ClaimResponse> response = pendingClaims.stream()
                .map(ClaimResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/approve-claim/{claimId}")
    public ResponseEntity<?> approveClaim(@PathVariable Long claimId, Authentication authentication) {
        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        
        // Verify the claim is for this restaurant's food
        if (!claim.getFoodItem().getRestaurant().getId().equals(restaurant.getId())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: You can only approve claims for your own food"));
        }
        
        if (claim.getStatus() != Claim.Status.PENDING) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Claim is not in pending status"));
        }
        
        claim.setStatus(Claim.Status.APPROVED);
        claim.setApprovedDate(LocalDateTime.now());
        
        // Mark food item as claimed
        FoodItem foodItem = claim.getFoodItem();
        foodItem.setStatus(FoodItem.Status.CLAIMED);
        foodItemRepository.save(foodItem);
        
        claimRepository.save(claim);
        
        return ResponseEntity.ok(new MessageResponse("Claim approved successfully!"));
    }
    
    @PutMapping("/reject-claim/{claimId}")
    public ResponseEntity<?> rejectClaim(@PathVariable Long claimId, 
                                          @RequestBody(required = false) RejectionRequest request,
                                          Authentication authentication) {
        User restaurant = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        
        // Verify the claim is for this restaurant's food
        if (!claim.getFoodItem().getRestaurant().getId().equals(restaurant.getId())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: You can only reject claims for your own food"));
        }
        
        if (claim.getStatus() != Claim.Status.PENDING) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Claim is not in pending status"));
        }
        
        claim.setStatus(Claim.Status.REJECTED);
        if (request != null && request.getReason() != null) {
            claim.setRejectionReason(request.getReason());
        }
        
        // Food item becomes available again (POSTED)
        FoodItem foodItem = claim.getFoodItem();
        foodItem.setStatus(FoodItem.Status.POSTED);
        foodItemRepository.save(foodItem);
        
        claimRepository.save(claim);
        
        return ResponseEntity.ok(new MessageResponse("Claim rejected successfully"));
    }
}
