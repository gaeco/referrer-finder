package net.gaeco.refererfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        log.info("Starting Git Function Analyzer");
        
        // Check if we have the required arguments
        if (args.length < 3) {
            log.error("Usage: java Main <repository-path> <old-commit-id> <new-commit-id>");
            log.error("Example: java Main /path/to/repo abc123 def456");
            System.exit(1);
        }
        
        String repositoryPath = args[0];
        String oldCommitId = args[1];
        String newCommitId = args[2];
        
        log.info("Analyzing repository: {}", repositoryPath);
        log.info("Comparing commits: {} -> {}", oldCommitId, newCommitId);
        
        GitFunctionAnalyzer analyzer = null;
        try {
            // Initialize the analyzer
            analyzer = new GitFunctionAnalyzer(repositoryPath);
            
            // Analyze function changes
            GitFunctionAnalyzer.FunctionChangeResult result = analyzer.analyzeFunctionChanges(oldCommitId, newCommitId);
            
            // Display results
            displayResults(result);
            
        } catch (IOException e) {
            log.error("Failed to initialize Git repository: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Error during analysis: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            // Clean up resources
            if (analyzer != null) {
                analyzer.close();
                log.info("Analysis completed and resources cleaned up");
            }
        }
    }
    
    /**
     * Displays the analysis results in a formatted way
     */
    private static void displayResults(GitFunctionAnalyzer.FunctionChangeResult result) {
        log.info("=== Function Change Analysis Results ===");
        log.info("Old Commit: {}", result.getOldCommitId());
        log.info("New Commit: {}", result.getNewCommitId());
        log.info("Total Changes: {} functions", 
                result.getAddedFunctions().size() + 
                result.getDeletedFunctions().size() + 
                result.getChangedFunctions().size());
        
        // Display added functions
        if (!result.getAddedFunctions().isEmpty()) {
            log.info("=== ADDED Functions ({}) ===", result.getAddedFunctions().size());
            result.getAddedFunctions().forEach(function -> 
                log.info("  + {}", function));
        }
        
        // Display deleted functions
        if (!result.getDeletedFunctions().isEmpty()) {
            log.info("=== DELETED Functions ({}) ===", result.getDeletedFunctions().size());
            result.getDeletedFunctions().forEach(function -> 
                log.info("  - {}", function));
        }
        
        // Display changed functions
        if (!result.getChangedFunctions().isEmpty()) {
            log.info("=== CHANGED Functions ({}) ===", result.getChangedFunctions().size());
            result.getChangedFunctions().forEach(function -> 
                log.info("  * {}", function));
        }
        
        if (result.getAddedFunctions().isEmpty() && 
            result.getDeletedFunctions().isEmpty() && 
            result.getChangedFunctions().isEmpty()) {
            log.info("No function changes detected in the target package");
        }
        
        log.info("=== Analysis Complete ===");
    }
}