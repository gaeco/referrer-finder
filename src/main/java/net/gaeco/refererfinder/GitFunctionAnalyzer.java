package net.gaeco.refererfinder;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core logic class for analyzing changed functions between two git commits
 */
public class GitFunctionAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(GitFunctionAnalyzer.class);
    private static final String TARGET_PACKAGE = "com.example.myapp";
    
    private final Repository repository;
    private final JavaParser javaParser;
    
    public GitFunctionAnalyzer(String repositoryPath) throws IOException {
        this.repository = Git.open(Paths.get(repositoryPath).toFile()).getRepository();
        this.javaParser = new JavaParser();
    }
    
    /**
     * Analyzes function changes between two git commits
     * 
     * @param oldCommitId the old commit ID
     * @param newCommitId the new commit ID
     * @return FunctionChangeResult containing the analysis results
     */
    public FunctionChangeResult analyzeFunctionChanges(String oldCommitId, String newCommitId) {
        logger.info("Analyzing function changes between commits: {} and {}", oldCommitId, newCommitId);
        
        try {
            // Get the two commits
            ObjectId oldId = repository.resolve(oldCommitId);
            ObjectId newId = repository.resolve(newCommitId);
            
            if (oldId == null || newId == null) {
                throw new IllegalArgumentException("Invalid commit IDs provided");
            }
            
            // Get changed Java files
            List<String> changedJavaFiles = getChangedJavaFiles(oldId, newId);
            logger.info("Found {} changed Java files", changedJavaFiles.size());
            
            // Analyze function changes for each file
            FunctionChangeResult result = new FunctionChangeResult();
            result.setOldCommitId(oldCommitId);
            result.setNewCommitId(newCommitId);
            
            for (String javaFile : changedJavaFiles) {
                analyzeFileFunctionChanges(javaFile, oldId, newId, result);
            }
            
            logger.info("Analysis completed. Added: {}, Deleted: {}, Changed: {}", 
                       result.getAddedFunctions().size(),
                       result.getDeletedFunctions().size(),
                       result.getChangedFunctions().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing function changes", e);
            throw new RuntimeException("Failed to analyze function changes", e);
        }
    }
    
    /**
     * Gets list of changed Java files between two commits
     */
    private List<String> getChangedJavaFiles(ObjectId oldId, ObjectId newId) throws IOException, GitAPIException {
        List<String> changedFiles = new ArrayList<>();
        
        try (RevWalk revWalk = new RevWalk(repository);
             ObjectReader reader = repository.newObjectReader()) {
            
            RevCommit oldCommit = revWalk.parseCommit(oldId);
            RevCommit newCommit = revWalk.parseCommit(newId);
            
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            
            oldTree.reset(reader, oldCommit.getTree());
            newTree.reset(reader, newCommit.getTree());
            
            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            
            List<DiffEntry> diffs = diffFormatter.scan(oldTree, newTree);
            
            for (DiffEntry diff : diffs) {
                String filePath = diff.getNewPath();
                if (filePath != null && filePath.endsWith(".java") && 
                    filePath.contains(TARGET_PACKAGE.replace('.', '/'))) {
                    changedFiles.add(filePath);
                }
            }
        }
        
        return changedFiles;
    }
    
    /**
     * Analyzes function changes in a specific Java file
     */
    private void analyzeFileFunctionChanges(String javaFile, ObjectId oldId, ObjectId newId, 
                                         FunctionChangeResult result) throws IOException {
        logger.debug("Analyzing function changes in file: {}", javaFile);
        
        // Get file content for both commits
        String oldContent = getFileContent(javaFile, oldId);
        String newContent = getFileContent(javaFile, newId);
        
        if (oldContent == null && newContent == null) {
            return; // File doesn't exist in either commit
        }
        
        // Parse Java files
        Set<String> oldFunctions = parseFunctions(oldContent);
        Set<String> newFunctions = parseFunctions(newContent);
        
        // Find added, deleted, and changed functions
        Set<String> addedFunctions = new HashSet<>(newFunctions);
        addedFunctions.removeAll(oldFunctions);
        
        Set<String> deletedFunctions = new HashSet<>(oldFunctions);
        deletedFunctions.removeAll(newFunctions);
        
        Set<String> commonFunctions = new HashSet<>(oldFunctions);
        commonFunctions.retainAll(newFunctions);
        
        // Add results
        for (String function : addedFunctions) {
            result.addAddedFunction(javaFile + "::" + function);
        }
        
        for (String function : deletedFunctions) {
            result.addDeletedFunction(javaFile + "::" + function);
        }
        
        // For changed functions, we need to compare method signatures more deeply
        for (String function : commonFunctions) {
            if (hasFunctionChanged(javaFile, function, oldContent, newContent)) {
                result.addChangedFunction(javaFile + "::" + function);
            }
        }
    }
    
    /**
     * Gets file content for a specific commit
     */
    private String getFileContent(String filePath, ObjectId commitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository);
             ObjectReader reader = repository.newObjectReader()) {
            
            RevCommit commit = revWalk.parseCommit(commitId);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, commit.getTree());
            
            // Find the file in the tree
            while (treeParser.next() != null) {
                String entryPath = treeParser.getEntryPathString();
                if (entryPath.equals(filePath)) {
                    // Read file content
                    byte[] content = reader.open(treeParser.getEntryObjectId()).getBytes();
                    return new String(content);
                }
            }
            
            return null; // File not found
        }
    }
    
    /**
     * Parses functions from Java source code
     */
    private Set<String> parseFunctions(String javaContent) {
        Set<String> functions = new HashSet<>();
        
        if (javaContent == null || javaContent.trim().isEmpty()) {
            return functions;
        }
        
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaContent);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                // Visit all classes and extract method names
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                        super.visit(n, arg);
                        
                        // Get all methods in this class
                        n.getMethods().forEach(method -> {
                            String methodName = method.getNameAsString();
                            String className = n.getNameAsString();
                            functions.add(className + "." + methodName);
                        });
                    }
                }, null);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Java content: {}", e.getMessage());
        }
        
        return functions;
    }
    
    /**
     * Checks if a function has changed between two versions
     */
    private boolean hasFunctionChanged(String filePath, String functionName, String oldContent, String newContent) {
        // Simple implementation - in a real scenario, you'd want more sophisticated comparison
        // This checks if the method signature or body has changed
        
        try {
            Set<String> oldMethodSignatures = extractMethodSignatures(oldContent, functionName);
            Set<String> newMethodSignatures = extractMethodSignatures(newContent, functionName);
            
            return !oldMethodSignatures.equals(newMethodSignatures);
        } catch (Exception e) {
            logger.warn("Failed to compare function changes for {}: {}", functionName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Extracts method signatures for comparison
     */
    private Set<String> extractMethodSignatures(String javaContent, String functionName) {
        Set<String> signatures = new HashSet<>();
        
        if (javaContent == null || javaContent.trim().isEmpty()) {
            return signatures;
        }
        
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(javaContent);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                cu.accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodDeclaration n, Void arg) {
                        super.visit(n, arg);
                        
                        String className = n.findAncestor(ClassOrInterfaceDeclaration.class)
                                         .map(ClassOrInterfaceDeclaration::getNameAsString)
                                         .orElse("");
                        String methodName = n.getNameAsString();
                        
                        if ((className + "." + methodName).equals(functionName)) {
                            // Create a signature that includes parameters and return type
                            String signature = n.getDeclarationAsString();
                            signatures.add(signature);
                        }
                    }
                }, null);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract method signatures: {}", e.getMessage());
        }
        
        return signatures;
    }
    
    /**
     * Closes the repository
     */
    public void close() {
        if (repository != null) {
            repository.close();
        }
    }
    
    /**
     * Result class for function change analysis
     */
    public static class FunctionChangeResult {
        private String oldCommitId;
        private String newCommitId;
        private final Set<String> addedFunctions = new HashSet<>();
        private final Set<String> deletedFunctions = new HashSet<>();
        private final Set<String> changedFunctions = new HashSet<>();
        
        public void addAddedFunction(String function) {
            addedFunctions.add(function);
        }
        
        public void addDeletedFunction(String function) {
            deletedFunctions.add(function);
        }
        
        public void addChangedFunction(String function) {
            changedFunctions.add(function);
        }
        
        // Getters and setters
        public String getOldCommitId() { return oldCommitId; }
        public void setOldCommitId(String oldCommitId) { this.oldCommitId = oldCommitId; }
        
        public String getNewCommitId() { return newCommitId; }
        public void setNewCommitId(String newCommitId) { this.newCommitId = newCommitId; }
        
        public Set<String> getAddedFunctions() { return new HashSet<>(addedFunctions); }
        public Set<String> getDeletedFunctions() { return new HashSet<>(deletedFunctions); }
        public Set<String> getChangedFunctions() { return new HashSet<>(changedFunctions); }
        
        @Override
        public String toString() {
            return String.format("FunctionChangeResult{oldCommit='%s', newCommit='%s', added=%d, deleted=%d, changed=%d}",
                    oldCommitId, newCommitId, addedFunctions.size(), deletedFunctions.size(), changedFunctions.size());
        }
    }
}
