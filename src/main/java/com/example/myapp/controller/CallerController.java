package com.example.myapp.controller;

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

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.info("Health check requested");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Caller");
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
            // TODO: Implement the actual analysis logic
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Analysis completed");
            response.put("oldCommit", request.get("oldCommit"));
            response.put("newCommit", request.get("newCommit"));
            
            // Placeholder for actual results
            response.put("addedFunctions", new String[0]);
            response.put("deletedFunctions", new String[0]);
            response.put("changedFunctions", new String[0]);
            
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("repository", "caller");
        response.put("description", "Utility to find changed functions between git commits");
        
        return ResponseEntity.ok(response);
    }
}
