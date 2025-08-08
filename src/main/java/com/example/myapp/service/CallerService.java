package com.example.myapp.service;

import net.gaeco.refererfinder.GitFunctionAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Service class for Caller utility
 * Handles business logic for analyzing function changes between git commits
 */
@Service
public class CallerService {

    private static final Logger logger = LoggerFactory.getLogger(CallerService.class);
    private static final String REPOSITORY_PATH = "."; // Current directory as default

    /**
     * Analyzes function changes between two git commits
     * 
     * @param oldCommitId the old commit ID
     * @param newCommitId the new commit ID
     * @return Map containing the analysis results
     */
    public Map<String, Object> analyzeFunctionChanges(String oldCommitId, String newCommitId) {
        logger.info("Service: Analyzing function changes between commits: {} and {}", oldCommitId, newCommitId);
        
        Map<String, Object> result = new HashMap<>();
        
        GitFunctionAnalyzer analyzer = null;
        try {
            analyzer = new GitFunctionAnalyzer(REPOSITORY_PATH);
            
            GitFunctionAnalyzer.FunctionChangeResult analysisResult = 
                analyzer.analyzeFunctionChanges(oldCommitId, newCommitId);
            
            result.put("status", "success");
            result.put("oldCommit", analysisResult.getOldCommitId());
            result.put("newCommit", analysisResult.getNewCommitId());
            result.put("addedFunctions", analysisResult.getAddedFunctions().toArray(new String[0]));
            result.put("deletedFunctions", analysisResult.getDeletedFunctions().toArray(new String[0]));
            result.put("changedFunctions", analysisResult.getChangedFunctions().toArray(new String[0]));
            
            logger.info("Service: Analysis completed successfully");
            
        } catch (Exception e) {
            logger.error("Service: Error analyzing function changes", e);
            result.put("status", "error");
            result.put("message", "Failed to analyze changes: " + e.getMessage());
        } finally {
            if (analyzer != null) {
                analyzer.close();
            }
        }
        
        return result;
    }

    /**
     * Gets repository information
     * 
     * @return Map containing repository information
     */
    public Map<String, Object> getRepositoryInfo() {
        logger.info("Service: Repository info requested");
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("repository", "caller");
        result.put("description", "Utility to find changed functions between git commits");
        result.put("targetPackage", "com.example.myapp");
        
        return result;
    }

    /**
     * Performs a health check
     * 
     * @return Map containing health status
     */
    public Map<String, Object> performHealthCheck() {
        logger.info("Service: Health check requested");
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "Caller");
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
}
