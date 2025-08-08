package com.example.myapp.controller;

import com.example.myapp.service.CallerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * Controller for Caller utility
 * Provides REST endpoints to analyze function changes between git commits
 */
@RestController
@RequestMapping("/api/caller")
public class CallerController {

    private static final Logger logger = LoggerFactory.getLogger(CallerController.class);
    
    @Autowired
    private CallerService callerService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("Health check requested");
        Map<String, Object> response = callerService.performHealthCheck();
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze function changes between two git commits
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeChanges(
            @RequestBody Map<String, String> request) {
        
        logger.info("Analyzing changes between commits: {} and {}", 
                   request.get("oldCommit"), request.get("newCommit"));
        
        try {
            String oldCommit = request.get("oldCommit");
            String newCommit = request.get("newCommit");
            
            if (oldCommit == null || newCommit == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Both oldCommit and newCommit are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = callerService.analyzeFunctionChanges(oldCommit, newCommit);
            
            if ("error".equals(response.get("status"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing changes", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to analyze changes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get repository information
     */
    @GetMapping("/repository")
    public ResponseEntity<Map<String, Object>> getRepositoryInfo() {
        logger.info("Repository info requested");
        Map<String, Object> response = callerService.getRepositoryInfo();
        return ResponseEntity.ok(response);
    }
}
