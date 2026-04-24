package org.codeforamerica.shiba;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class UnusedMessagesTest {

    private static final String RESOURCES_PATH = "src/main/resources";
    private static final String MESSAGES_FILE = "messages.properties";
    private static final String PAGES_CONFIG_FILE = "pages-config.yaml";
    private static final String TEMPLATES_PATH = "templates";

    @Disabled
    @Test
    public void testNoUnusedMessages() throws IOException {
        // Load all message keys from messages.properties
        Set<String> messageKeys = loadMessageKeys();
        System.out.println("Total message keys found: " + messageKeys.size());

        // Load content from all relevant files
        Set<String> javaFileContent = loadJavaFilesContent();
        Set<String> resourceFileContent = loadResourceFilesContent();
        Set<String> htmlFileContent = loadHtmlFilesContent();
        
        System.out.println("Loaded " + javaFileContent.size() + " Java files");
        System.out.println("Loaded " + resourceFileContent.size() + " Resource files");
        System.out.println("Loaded " + htmlFileContent.size() + " HTML files");
        
        // Find unused messages
        Set<String> unusedMessages = new HashSet<>(messageKeys);
        
        for (String key : messageKeys) {
            boolean found = false;
            
            // Check in Java files
            for (String content : javaFileContent) {
                if (isKeyUsed(content, key)) {
                    found = true;
                    break;
                }
            }
            
            if (found) {
                unusedMessages.remove(key);
                continue;
            }
            
            // Check in resource files (YAML, properties, XML, etc.)
            for (String content : resourceFileContent) {
                if (isKeyUsed(content, key)) {
                    found = true;
                    break;
                }
            }
            
            if (found) {
                unusedMessages.remove(key);
                continue;
            }
            
            // Check in HTML templates
            for (String content : htmlFileContent) {
                if (isKeyUsed(content, key)) {
                    found = true;
                    break;
                }
            }
            
            if (found) {
                unusedMessages.remove(key);
            }
        }
        
        // Print results
        if (!unusedMessages.isEmpty()) {
            System.out.println("\n=== UNUSED MESSAGE KEYS ===");
            unusedMessages.stream()
                    .sorted()
                    .forEach(key -> System.out.println("  - " + key));
            System.out.println("\nTotal unused: " + unusedMessages.size());
        } else {
            System.out.println("\n=== ALL MESSAGES ARE USED ===");
        }
        
        // Assertion - fail the test if any unused messages are found
        assertTrue(unusedMessages.isEmpty(), "Found unused message keys: " + unusedMessages);
    }

    /**
     * Load all message keys from messages.properties file
     */
    private Set<String> loadMessageKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        Path propertiesFile = Paths.get(RESOURCES_PATH, MESSAGES_FILE);
        
        if (!Files.exists(propertiesFile)) {
            fail("messages.properties file not found at: " + propertiesFile.toAbsolutePath());
        }
        
        List<String> lines = Files.readAllLines(propertiesFile, StandardCharsets.UTF_8);
        
        for (String line : lines) {
            line = line.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Extract the key (everything before the first =)
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                keys.add(key);
            }
        }
        
        return keys;
    }

    /**
     * Load content from all Java files in src/main/java
     */
    private Set<String> loadJavaFilesContent() throws IOException {
        Set<String> allContent = new HashSet<>();
        Path javaSourcePath = Paths.get("src/main/java");
        
        if (!Files.exists(javaSourcePath)) {
            System.out.println("Warning: Java source directory not found at: " + javaSourcePath.toAbsolutePath());
            return allContent;
        }
        
        try (Stream<Path> walk = Files.walk(javaSourcePath)) {
            List<Path> javaFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            
            for (Path javaFile : javaFiles) {
                try {
                    String content = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
                    allContent.add(content);
                } catch (IOException e) {
                    System.out.println("Warning: Could not read Java file: " + javaFile);
                }
            }
        }
        
        return allContent;
    }

    /**
     * Load content from all resource files (YAML, properties, XML, XSD, etc.)
     */
    private Set<String> loadResourceFilesContent() throws IOException {
        Set<String> allContent = new HashSet<>();
        Path resourcePath = Paths.get(RESOURCES_PATH);
        
        if (!Files.exists(resourcePath)) {
            System.out.println("Warning: Resources directory not found at: " + resourcePath.toAbsolutePath());
            return allContent;
        }
        
        try (Stream<Path> walk = Files.walk(resourcePath)) {
            List<Path> resourceFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        // Include YAML, XML, XSD, properties files but exclude templates and HTML
                        return (name.endsWith(".yaml") || name.endsWith(".yml") || 
                                name.endsWith(".xml") || name.endsWith(".xsd")) &&
                               !name.endsWith(".html");
                    })
                    .collect(Collectors.toList());
            
            for (Path resourceFile : resourceFiles) {
                try {
                    String content = new String(Files.readAllBytes(resourceFile), StandardCharsets.UTF_8);
                    allContent.add(content);
                } catch (IOException e) {
                    System.out.println("Warning: Could not read resource file: " + resourceFile);
                }
            }
        }
        
        return allContent;
    }

    /**
     * Load content from all HTML template files
     */
    private Set<String> loadHtmlFilesContent() throws IOException {
        Set<String> allContent = new HashSet<>();
        Path templatesPath = Paths.get(RESOURCES_PATH, TEMPLATES_PATH);
        
        if (!Files.exists(templatesPath)) {
            System.out.println("Warning: Templates directory not found at: " + templatesPath.toAbsolutePath());
            return allContent;
        }
        
        try (Stream<Path> walk = Files.walk(templatesPath)) {
            List<Path> htmlFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".html"))
                    .collect(Collectors.toList());
            
            for (Path htmlFile : htmlFiles) {
                try {
                    String content = new String(Files.readAllBytes(htmlFile), StandardCharsets.UTF_8);
                    allContent.add(content);
                } catch (IOException e) {
                    System.out.println("Warning: Could not read HTML file: " + htmlFile);
                }
            }
        }
        
        return allContent;
    }

    /**
     * Check if a message key is used in content.
     * Uses a simple boundary check: the character after the key must NOT be a dash (-), dot (.), digit, or letter.
     * This prevents false positives like:
     * - "prepare-to" matching in "prepare-to.apply-something"
     * - "test" matching in "test-value" or "test.value" or "test1" or "testing"
     * 
     * Follows the message naming convention: prepare-to.apply-something
     */
    private boolean isKeyUsed(String content, String key) {
        int index = 0;
        while ((index = content.indexOf(key, index)) != -1) {
            // Check character after the key
            int charAfterIndex = index + key.length();
            
            if (charAfterIndex >= content.length()) {
                // Key is at end of content - valid match
                return true;
            }
            
            char charAfter = content.charAt(charAfterIndex);
            
            // If the character after is NOT a dash, dot, digit, or letter, it's a valid match
            if (charAfter != '-' && charAfter != '.' && !Character.isDigit(charAfter) && !Character.isLetter(charAfter)) {
                return true;
            }
            
            // Move to next occurrence
            index++;
        }
        return false;
    }
}
