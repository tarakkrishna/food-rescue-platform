package com.foodrescue.controller;

import com.foodrescue.dto.AdminStatsDTO;
import com.foodrescue.entity.Claim;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.repository.ClaimRepository;
import com.foodrescue.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminStatsController {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getStats() {
        AdminStatsDTO stats = new AdminStatsDTO();
        
        // Count food items by status
        long totalFoodAdded = foodItemRepository.count();
        long totalFoodClaimed = foodItemRepository.countByStatus(FoodItem.Status.CLAIMED);
        long totalFoodExpired = foodItemRepository.countByStatus(FoodItem.Status.EXPIRED);
        long pendingClaimsCount = claimRepository.countByStatus(Claim.Status.PENDING);
        
        stats.setTotalFoodAdded(totalFoodAdded);
        stats.setTotalFoodClaimed(totalFoodClaimed);
        stats.setTotalFoodExpired(totalFoodExpired);
        stats.setPendingClaimsCount(pendingClaimsCount);
        
        // Get top restaurants by food added
        List<Object[]> topRestaurantsData = foodItemRepository.findTopRestaurantsByFoodAdded();
        List<AdminStatsDTO.TopUserDTO> topRestaurants = topRestaurantsData.stream()
                .limit(5)
                .map(obj -> new AdminStatsDTO.TopUserDTO(
                        (Long) obj[0],
                        (String) obj[1],
                        (Long) obj[2]
                ))
                .collect(Collectors.toList());
        stats.setTopRestaurants(topRestaurants);
        
        // Get top NGOs by approved claims
        List<Object[]> topNgosData = claimRepository.findTopNgosByClaims();
        List<AdminStatsDTO.TopUserDTO> topNgos = topNgosData.stream()
                .limit(5)
                .map(obj -> new AdminStatsDTO.TopUserDTO(
                        (Long) obj[0],
                        (String) obj[1],
                        (Long) obj[2]
                ))
                .collect(Collectors.toList());
        stats.setTopNgos(topNgos);
        
        return ResponseEntity.ok(stats);
    }
}
