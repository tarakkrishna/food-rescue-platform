package com.foodrescue.service;

import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.repository.FoodItemRepository;
import com.foodrescue.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VolunteerService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public FoodItem assignVolunteer(Long donationId, Long volunteerId, Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is NGO
        if (ngo.getRole() != User.Role.NGO) {
            throw new RuntimeException("Only NGOs can assign volunteers");
        }

        FoodItem foodItem = foodItemRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        // Check donation status is CLAIMED
        if (foodItem.getStatus() != FoodItem.Status.CLAIMED) {
            throw new RuntimeException("Donation must be in CLAIMED status to assign volunteer. Current status: " + foodItem.getStatus());
        }

        User volunteer = userRepository.findById(volunteerId)
                .orElseThrow(() -> new RuntimeException("Volunteer not found"));

        // Verify user is VOLUNTEER
        if (volunteer.getRole() != User.Role.VOLUNTEER) {
            throw new RuntimeException("Assigned user must be a VOLUNTEER");
        }

        // Assign volunteer and update status
        foodItem.setVolunteer(volunteer);
        foodItem.setStatus(FoodItem.Status.ASSIGNED);

        return foodItemRepository.save(foodItem);
    }

    @Transactional
    public FoodItem pickupDonation(Long donationId, Authentication authentication) {
        User volunteer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is VOLUNTEER
        if (volunteer.getRole() != User.Role.VOLUNTEER) {
            throw new RuntimeException("Only volunteers can pickup donations");
        }

        FoodItem foodItem = foodItemRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        // Check if volunteer is assigned to this donation
        if (foodItem.getVolunteer() == null || !foodItem.getVolunteer().getId().equals(volunteer.getId())) {
            throw new RuntimeException("You are not assigned to this donation");
        }

        // Check status is ASSIGNED
        if (foodItem.getStatus() != FoodItem.Status.ASSIGNED) {
            throw new RuntimeException("Donation must be in ASSIGNED status for pickup. Current status: " + foodItem.getStatus());
        }

        // Update status to PICKED_UP
        foodItem.setStatus(FoodItem.Status.PICKED_UP);

        return foodItemRepository.save(foodItem);
    }

    @Transactional
    public FoodItem deliverDonation(Long donationId, Authentication authentication) {
        User volunteer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is VOLUNTEER
        if (volunteer.getRole() != User.Role.VOLUNTEER) {
            throw new RuntimeException("Only volunteers can deliver donations");
        }

        FoodItem foodItem = foodItemRepository.findById(donationId)
                .orElseThrow(() -> new RuntimeException("Donation not found"));

        // Check if volunteer is assigned to this donation
        if (foodItem.getVolunteer() == null || !foodItem.getVolunteer().getId().equals(volunteer.getId())) {
            throw new RuntimeException("You are not assigned to this donation");
        }

        // Check status is PICKED_UP
        if (foodItem.getStatus() != FoodItem.Status.PICKED_UP) {
            throw new RuntimeException("Donation must be in PICKED_UP status for delivery. Current status: " + foodItem.getStatus());
        }

        // Update status to DELIVERED
        foodItem.setStatus(FoodItem.Status.DELIVERED);

        return foodItemRepository.save(foodItem);
    }

    public List<FoodItem> getMyAssignedDonations(Authentication authentication) {
        User volunteer = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is VOLUNTEER
        if (volunteer.getRole() != User.Role.VOLUNTEER) {
            throw new RuntimeException("Only volunteers can view assigned donations");
        }

        return foodItemRepository.findByVolunteer(volunteer);
    }
}
