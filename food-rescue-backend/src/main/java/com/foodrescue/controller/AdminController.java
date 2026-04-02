package com.foodrescue.controller;

import com.foodrescue.dto.MessageResponse;
import com.foodrescue.entity.User;
import com.foodrescue.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/pending-ngos")
    public ResponseEntity<List<User>> getPendingNgos() {
        List<User> pendingNgos = userRepository.findByRoleAndTrusted(User.Role.NGO, false);
        return ResponseEntity.ok(pendingNgos);
    }

    @PutMapping("/approve-ngo/{ngoId}")
    public ResponseEntity<MessageResponse> approveNgo(@PathVariable Long ngoId) {
        User ngo = userRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found"));
        
        if (ngo.getRole() != User.Role.NGO) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("User is not an NGO"));
        }
        
        ngo.setTrusted(true);
        userRepository.save(ngo);
        
        return ResponseEntity.ok(new MessageResponse("NGO approved successfully"));
    }

    @PutMapping("/reject-ngo/{ngoId}")
    public ResponseEntity<MessageResponse> rejectNgo(@PathVariable Long ngoId) {
        User ngo = userRepository.findById(ngoId)
                .orElseThrow(() -> new RuntimeException("NGO not found"));
        
        if (ngo.getRole() != User.Role.NGO) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("User is not an NGO"));
        }
        
        // Delete the NGO user or mark as rejected
        // For now, we'll just mark them as not trusted (rejected)
        ngo.setTrusted(false);
        userRepository.save(ngo);
        
        return ResponseEntity.ok(new MessageResponse("NGO rejected"));
    }
}
