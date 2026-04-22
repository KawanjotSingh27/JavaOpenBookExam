package com.portal;

import com.portal.db.DBConnectionPool;
import com.portal.db.FileMetadata;
import com.portal.db.FileUploadService;
import com.portal.di.ReflectionLoader;
import com.portal.encryption.EncryptionStrategy;
import com.portal.parser.FileParser;
import com.portal.parser.ParserFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * PortalMain — demonstrates the full upload workflow using all four patterns:
 *
 *   1. FACTORY PATTERN   — ParserFactory.getParserForFile() selects parser.
 *   2. STRATEGY PATTERN  — EncryptionFactory.getStrategy() selects cipher.
 *   3. MANUAL DI         — ReflectionLoader.loadParser() loads at runtime.
 *   4. SINGLETON PATTERN — DBConnectionPool.getInstance() used by service.
 *   5. JDBC TRANSACTION  — FileUploadService.uploadFile() wraps in one tx.
 */
public class PortalMain {

    public static void main(String[] args) throws Exception {

        System.out.println("=== Student Resource Portal Starting ===\n");

        // ── One-time startup ──────────────────────────────────────────────────
        FileUploadService service = new FileUploadService();
        FileUploadService.initialiseSchema();

        // ── Demo upload 1: a Markdown lecture note (AES-128 encryption) ───────
        simulateUpload(service,
                "sample_lecture.md",
                "md",
                "Prof. Sharma",
                "AES-128",
                "lecture notes content in markdown format");

        // ── Demo upload 2: a Text file (no encryption — public resource) ──────
        simulateUpload(service,
                "syllabus.txt",
                "txt",
                "Prof. Mehta",
                "NONE",
                "course syllabus plain text content");

        // ── Demo upload 3: load a parser via Reflection (Manual DI) ──────────
        System.out.println("\n--- Manual DI Demo: Loading parser via Reflection ---");
        // Register a hypothetical external DocxParser at runtime
        ReflectionLoader.registerParser("txt", "com.portal.parser.TextParser");
        // Load it dynamically — simulates a plugin loaded from a folder
        Object dynamicParser = ReflectionLoader.loadParser("txt", "./plugins");
        // Cast through the interface — decoupled from the concrete class
        if (dynamicParser instanceof FileParser fp) {
            System.out.println("[DI] Dynamically loaded: " + fp.getSupportedFormat());
        }

        // ── Shutdown ──────────────────────────────────────────────────────────
        DBConnectionPool.getInstance().closeAll();
        System.out.println("\n=== Portal Shutdown Complete ===");
    }

    /**
     * Helper: builds a temp file, parses it, encrypts it, and uploads it.
     *
     * @param service         Upload service instance.
     * @param fileName        File name with extension.
     * @param ext             File extension without dot.
     * @param faculty         Faculty username for audit trail.
     * @param encryptionLevel "NONE", "AES-128", or "AES-256".
     * @param content         Dummy file content string.
     */
    private static void simulateUpload(FileUploadService service,
                                       String fileName,
                                       String ext,
                                       String faculty,
                                       String encryptionLevel,
                                       String content) throws Exception {

        System.out.println("\n─── Uploading: " + fileName + " ───────────────────────");

        // 1. Write a temp file with sample content
        Files.writeString(Paths.get(fileName), content);
        byte[] rawBytes = content.getBytes();

        // 2. FACTORY PATTERN — choose the right parser without if/else
        FileParser parser = ParserFactory.getParserForFile(fileName);
        String parsedContent = parser.parse(fileName);
        System.out.println("[Parser] Extracted: " + parsedContent.substring(0, Math.min(60, parsedContent.length())) + "...");

        // 3. STRATEGY PATTERN — choose the right encryption algorithm
        EncryptionStrategy strategy = com.portal.encryption.EncryptionFactory.getStrategy(encryptionLevel);
        byte[] encryptedBytes = strategy.encrypt(rawBytes);

        // 4. Build metadata value object
        FileMetadata metadata = new FileMetadata(
                fileName,
                ext,
                rawBytes.length,
                strategy.levelDescription(),
                faculty,
                LocalDateTime.now().toString()
        );

        // 5. JDBC TRANSACTION — upload is atomic (file write + DB insert)
        boolean success = service.uploadFile(fileName, encryptedBytes, metadata);
        System.out.println("[Result] Upload " + (success ? "SUCCEEDED ✓" : "FAILED ✗"));

        // Clean up temp file
        Files.deleteIfExists(Paths.get(fileName));
    }
}
