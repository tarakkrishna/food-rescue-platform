package com.foodrescue.controller;

import com.foodrescue.dto.FoodItemResponse;
import com.foodrescue.dto.MessageResponse;
import com.foodrescue.entity.Claim;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.repository.ClaimRepository;
import com.foodrescue.repository.FoodItemRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.LocationService;
import com.foodrescue.service.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/volunteer")
@CrossOrigin(origins = "http://localhost:3000")
public class VolunteerController {

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private LocationService locationService;

    @PutMapping("/donations/{donationId}/assign/{volunteerId}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> assignVolunteer(@PathVariable Long donationId, 
                                              @PathVariable Long volunteerId,
                                              Authentication authentication) {
        try {
            FoodItem foodItem = volunteerService.assignVolunteer(donationId, volunteerId, authentication);
            return ResponseEntity.ok(new MessageResponse("Volunteer assigned successfully! Status updated to ASSIGNED"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/donations/{donationId}/pickup")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> pickupDonation(@PathVariable Long donationId, 
                                             Authentication authentication) {
        try {
            FoodItem foodItem = volunteerService.pickupDonation(donationId, authentication);
            return ResponseEntity.ok(new MessageResponse("Donation picked up successfully! Status updated to PICKED_UP"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/donations/{donationId}/deliver")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> deliverDonation(@PathVariable Long donationId, 
                                              Authentication authentication) {
        try {
            FoodItem foodItem = volunteerService.deliverDonation(donationId, authentication);
            return ResponseEntity.ok(new MessageResponse("Donation delivered successfully! Status updated to DELIVERED"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> getMyAssignments(Authentication authentication) {
        try {
            List<FoodItem> foodItems = volunteerService.getMyAssignedDonations(authentication);
            List<FoodItemResponse> response = foodItems.stream()
                    .map(FoodItemResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/nearby-tasks")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> getNearbyTasks(
            Authentication authentication,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (volunteer.getLatitude() == null || volunteer.getLongitude() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please update your location in profile"));
            }

            List<FoodItem> unassignedDonations = foodItemRepository.findUnassignedDonationsWithLocation();

            List<Map<String, Object>> nearbyTasks = unassignedDonations.stream()
                    .filter(d -> locationService.isWithinRadius(
                            volunteer.getLatitude(), volunteer.getLongitude(), d, radiusKm))
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
                        map.put("distanceKm", locationService.getDistance(
                                volunteer.getLatitude(), volunteer.getLongitude(), d));
                        return map;
                    })
                    .sorted((d1, d2) -> Double.compare((Double) d1.get("distanceKm"), (Double) d2.get("distanceKm")))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(nearbyTasks);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/nearby-pickups")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> getNearbyPickups(
            Authentication authentication,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (volunteer.getLatitude() == null || volunteer.getLongitude() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please update your location in profile"));
            }

            // Get PENDING claims (food claimed but not picked up yet)
            List<Claim> pendingClaims = claimRepository.findByStatus(Claim.Status.PENDING);

            List<Map<String, Object>> nearbyPickups = pendingClaims.stream()
                    .filter(claim -> locationService.isWithinRadius(
                            volunteer.getLatitude(), volunteer.getLongitude(), 
                            claim.getFoodItem(), radiusKm))
                    .map(claim -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", claim.getId());
                        map.put("foodItemName", claim.getFoodItem().getName());
                        map.put("quantity", claim.getFoodItem().getQuantity());
                        map.put("restaurantName", claim.getFoodItem().getRestaurant().getName());
                        map.put("ngoName", claim.getNgo().getName());
                        map.put("restaurantLatitude", claim.getFoodItem().getLatitude());
                        map.put("restaurantLongitude", claim.getFoodItem().getLongitude());
                        map.put("ngoLatitude", claim.getNgo().getLatitude());
                        map.put("ngoLongitude", claim.getNgo().getLongitude());
                        map.put("distanceKm", locationService.getDistance(
                                volunteer.getLatitude(), volunteer.getLongitude(), 
                                claim.getFoodItem()));
                        return map;
                    })
                    .sorted((d1, d2) -> Double.compare((Double) d1.get("distanceKm"), (Double) d2.get("distanceKm")))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(nearbyPickups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/nearby-drops")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> getNearbyDrops(
            Authentication authentication,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (volunteer.getLatitude() == null || volunteer.getLongitude() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please update your location in profile"));
            }

            // Get PICKED_UP claims (food picked up but not delivered yet)
            List<Claim> pickedUpClaims = claimRepository.findByStatus(Claim.Status.PICKED_UP);

            List<Map<String, Object>> nearbyDrops = pickedUpClaims.stream()
                    .filter(claim -> claim.getVolunteer() != null && 
                            claim.getVolunteer().getId().equals(volunteer.getId()))
                    .filter(claim -> {
                        if (claim.getNgo().getLatitude() == null || claim.getNgo().getLongitude() == null) {
                            return false;
                        }
                        double distance = locationService.calculateDistance(
                            volunteer.getLatitude(), volunteer.getLongitude(),
                            claim.getNgo().getLatitude(), claim.getNgo().getLongitude()
                        );
                        return distance <= radiusKm;
                    })
                    .map(claim -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", claim.getId());
                        map.put("foodItemName", claim.getFoodItem().getName());
                        map.put("quantity", claim.getFoodItem().getQuantity());
                        map.put("restaurantName", claim.getFoodItem().getRestaurant().getName());
                        map.put("ngoName", claim.getNgo().getName());
                        map.put("pickupLatitude", claim.getFoodItem().getLatitude());
                        map.put("pickupLongitude", claim.getFoodItem().getLongitude());
                        map.put("dropLatitude", claim.getNgo().getLatitude());
                        map.put("dropLongitude", claim.getNgo().getLongitude());
                        map.put("distanceKm", locationService.calculateDistance(
                                volunteer.getLatitude(), volunteer.getLongitude(), 
                                claim.getNgo().getLatitude(), claim.getNgo().getLongitude()));
                        return map;
                    })
                    .sorted((d1, d2) -> Double.compare((Double) d1.get("distanceKm"), (Double) d2.get("distanceKm")))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(nearbyDrops);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/accept-pickup/{claimId}")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> acceptPickupTask(@PathVariable Long claimId, Authentication authentication) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Claim claim = claimRepository.findById(claimId)
                    .orElseThrow(() -> new RuntimeException("Claim not found"));

            if (claim.getStatus() != Claim.Status.PENDING) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This task is no longer available"));
            }

            if (claim.getVolunteer() != null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This task has already been accepted"));
            }

            // Assign volunteer to claim
            claim.setVolunteer(volunteer);
            claimRepository.save(claim);

            return ResponseEntity.ok(new MessageResponse("Pickup task accepted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/accept-delivery/{claimId}")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> acceptDeliveryTask(@PathVariable Long claimId, Authentication authentication) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Claim claim = claimRepository.findById(claimId)
                    .orElseThrow(() -> new RuntimeException("Claim not found"));

            if (claim.getStatus() != Claim.Status.PICKED_UP) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This task is not ready for delivery"));
            }

            if (!claim.getVolunteer().getId().equals(volunteer.getId())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("You are not assigned to this task"));
            }

            return ResponseEntity.ok(new MessageResponse("Delivery task confirmed! You can proceed with delivery."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
