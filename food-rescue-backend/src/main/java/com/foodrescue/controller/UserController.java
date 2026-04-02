package com.foodrescue.controller;

import com.foodrescue.dto.MessageResponse;
import com.foodrescue.dto.UserLocationRequest;
import com.foodrescue.entity.User;
import com.foodrescue.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/update-location")
    @Transactional
    public ResponseEntity<?> updateLocation(@RequestBody UserLocationRequest request) {
        // Defensive null check for request
        if (request == null) {
            logger.error("Request body is NULL - JSON parsing failed");
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Request body is required"));
        }
        
        logger.info("Request received: latitude={}, longitude={}", request.getLatitude(), request.getLongitude());
        
        try {
            // Debug: Log incoming request
            logger.info("Received location update request - Latitude: {}, Longitude: {}", 
                request.getLatitude(), request.getLongitude());

            // Explicit null validation
            if (request.getLatitude() == null || request.getLongitude() == null) {
                logger.error("Validation failed: Latitude or Longitude is null");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Latitude and Longitude are required"));
            }

            // Get currently logged-in user using SecurityContextHolder
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.error("User not authenticated - auth={}, name={}", auth, auth != null ? auth.getName() : "null");
                return ResponseEntity.status(401).body(new MessageResponse("Error: User not authenticated"));
            }
            String email = auth.getName();
            logger.info("Authenticated user email: {}", email);
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found with email: {}", email);
                        return new RuntimeException("User not found");
                    });
            
            logger.info("Found user - ID: {}, Email: {}", user.getId(), user.getEmail());

            // Validate latitude range (-90 to 90)
            if (request.getLatitude() < -90 || request.getLatitude() > 90) {
                logger.error("Validation failed: Latitude {} out of range", request.getLatitude());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Latitude must be between -90 and 90"));
            }

            // Validate longitude range (-180 to 180)
            if (request.getLongitude() < -180 || request.getLongitude() > 180) {
                logger.error("Validation failed: Longitude {} out of range", request.getLongitude());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Longitude must be between -180 and 180"));
            }

            // Update location
            user.setLatitude(request.getLatitude());
            user.setLongitude(request.getLongitude());
            logger.info("Updating user location - Lat: {}, Lon: {}", 
                request.getLatitude(), request.getLongitude());

            // Save to database
            User savedUser = userRepository.save(user);
            logger.info("User saved successfully - ID: {}, Lat: {}, Lon: {}", 
                savedUser.getId(), savedUser.getLatitude(), savedUser.getLongitude());

            // Prepare response with updated user details
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("name", savedUser.getName());
            response.put("email", savedUser.getEmail());
            response.put("role", savedUser.getRole().name());
            response.put("latitude", savedUser.getLatitude());
            response.put("longitude", savedUser.getLongitude());
            response.put("message", "Location updated successfully!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Print full stack trace to identify root cause
            logger.error("FULL EXCEPTION - Error updating location:", e);
            
            // Print root cause
            Throwable cause = e.getCause();
            while (cause != null) {
                logger.error("ROOT CAUSE: {}", cause.getMessage(), cause);
                cause = cause.getCause();
            }
            
            return ResponseEntity.status(500).body(new MessageResponse("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());
            response.put("trusted", user.isTrusted());
            response.put("latitude", user.getLatitude());
            response.put("longitude", user.getLongitude());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
