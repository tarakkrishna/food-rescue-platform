package com.foodrescue.controller;

import com.foodrescue.dto.MessageResponse;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.entity.VolunteerRequest;
import com.foodrescue.repository.FoodItemRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.repository.VolunteerRequestRepository;
import com.foodrescue.service.LocationService;
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
@RequestMapping("/volunteer-requests")
@CrossOrigin(origins = "http://localhost:3000")
public class VolunteerRequestController {

    @Autowired
    private VolunteerRequestRepository volunteerRequestRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationService locationService;

    private static final double MAX_REQUEST_DISTANCE_KM = 20.0;

    @PostMapping("/request/{donationId}")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> requestDonation(@PathVariable Long donationId, Authentication authentication) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            FoodItem donation = foodItemRepository.findById(donationId)
                    .orElseThrow(() -> new RuntimeException("Donation not found"));

            // Validation: Cannot request if already assigned
            if (donation.getVolunteer() != null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This donation is already assigned to a volunteer"));
            }

            // Validation: Only POSTED or CLAIMED status can be requested
            if (donation.getStatus() != FoodItem.Status.POSTED && donation.getStatus() != FoodItem.Status.CLAIMED) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This donation is not available for volunteer requests"));
            }

            // Validation: Check if volunteer already requested this donation
            if (volunteerRequestRepository.existsByDonationAndVolunteer(donation, volunteer)) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("You have already requested this donation"));
            }

            // Validation: Check if volunteer has location set
            if (volunteer.getLatitude() == null || volunteer.getLongitude() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please update your location in profile before requesting"));
            }

            // Validation: Check if donation has location
            if (donation.getLatitude() == null || donation.getLongitude() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This donation does not have location information"));
            }

            // Validation: Check distance constraint
            double distance = locationService.calculateDistance(
                    volunteer.getLatitude(), volunteer.getLongitude(),
                    donation.getLatitude(), donation.getLongitude());

            if (distance > MAX_REQUEST_DISTANCE_KM) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("You are too far from this donation (max " + MAX_REQUEST_DISTANCE_KM + " km)"));
            }

            VolunteerRequest request = new VolunteerRequest(donation, volunteer);
            volunteerRequestRepository.save(request);

            return ResponseEntity.ok(new MessageResponse("Request submitted successfully! Status: PENDING"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<?> getMyRequests(Authentication authentication) {
        try {
            User volunteer = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<VolunteerRequest> requests = volunteerRequestRepository.findByVolunteer(volunteer);

            List<Map<String, Object>> response = requests.stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", r.getId());
                        map.put("donationId", r.getDonation().getId());
                        map.put("donationName", r.getDonation().getName());
                        map.put("quantity", r.getDonation().getQuantity());
                        map.put("status", r.getStatus());
                        map.put("createdAt", r.getCreatedAt());
                        map.put("restaurantName", r.getDonation().getRestaurant() != null ?
                                r.getDonation().getRestaurant().getName() : "Unknown");
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/donation/{donationId}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> getRequestsForDonation(@PathVariable Long donationId, Authentication authentication) {
        try {
            FoodItem donation = foodItemRepository.findById(donationId)
                    .orElseThrow(() -> new RuntimeException("Donation not found"));

            // Verify NGO owns the claim for this donation
            if (donation.getStatus() == FoodItem.Status.POSTED) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This donation has not been claimed yet"));
            }

            List<VolunteerRequest> requests = volunteerRequestRepository.findByDonation(donation);

            List<Map<String, Object>> response = requests.stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", r.getId());
                        map.put("volunteerId", r.getVolunteer().getId());
                        map.put("volunteerName", r.getVolunteer().getName());
                        map.put("volunteerEmail", r.getVolunteer().getEmail());
                        map.put("status", r.getStatus());
                        map.put("createdAt", r.getCreatedAt());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/approve/{requestId}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> approveRequest(@PathVariable Long requestId, Authentication authentication) {
        try {
            VolunteerRequest request = volunteerRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            FoodItem donation = request.getDonation();

            // Validation: Check if already assigned
            if (donation.getVolunteer() != null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("This donation is already assigned to another volunteer"));
            }

            // Approve this request
            request.setStatus(VolunteerRequest.Status.APPROVED);
            volunteerRequestRepository.save(request);

            // Assign volunteer to donation
            donation.setVolunteer(request.getVolunteer());
            donation.setStatus(FoodItem.Status.ASSIGNED);
            foodItemRepository.save(donation);

            // Reject all other requests for this donation
            List<VolunteerRequest> otherRequests = volunteerRequestRepository.findByDonation(donation);
            for (VolunteerRequest other : otherRequests) {
                if (!other.getId().equals(requestId)) {
                    other.setStatus(VolunteerRequest.Status.REJECTED);
                    volunteerRequestRepository.save(other);
                }
            }

            return ResponseEntity.ok(new MessageResponse(
                    "Request approved! Volunteer assigned to donation. Other requests rejected automatically."));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/reject/{requestId}")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId, Authentication authentication) {
        try {
            VolunteerRequest request = volunteerRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            request.setStatus(VolunteerRequest.Status.REJECTED);
            volunteerRequestRepository.save(request);

            return ResponseEntity.ok(new MessageResponse("Request rejected successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/open-donations-count")
    public ResponseEntity<?> getOpenDonationsCount() {
        long count = foodItemRepository.findUnassignedDonations().size();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
