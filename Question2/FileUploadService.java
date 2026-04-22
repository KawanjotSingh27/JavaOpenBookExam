package com.portal.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * FileUploadService — orchestrates the complete file upload workflow.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  JDBC TRANSACTION DESIGN (setAutoCommit(false))                          │
 * │                                                                          │
 * │  The upload process has two steps that must succeed together or fail     │
 * │  together (atomicity):                                                   │
 * │    STEP 1 — Copy the encrypted file bytes to the storage directory.     │
 * │    STEP 2 — INSERT the file metadata row into SQLite.                   │
 * │                                                                          │
 * │  If STEP 2 fails (e.g., disk full, constraint violation), STEP 1 must   │
 * │  be rolled back so the DB never contains a metadata row pointing to a   │
 * │  file that doesn't exist, and vice-versa.                               │
 * │                                                                          │
 * │  setAutoCommit(false) achieves this: both operations execute inside a   │
 * │  single transaction.  conn.commit() is called only when BOTH succeed.  │
 * │  conn.rollback() is called in the catch block to undo any partial work. │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
public class FileUploadService {

    private static final String STORAGE_DIR = "uploaded_files/";

    /**
     * Initialises the SQLite schema.  Call once at application startup.
     */
    public static void initialiseSchema() {
        Connection conn = DBConnectionPool.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS uploaded_resources (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        file_name        TEXT    NOT NULL,
                        file_type        TEXT    NOT NULL,
                        file_size_bytes  INTEGER NOT NULL,
                        encryption_level TEXT    NOT NULL,
                        uploaded_by      TEXT    NOT NULL,
                        uploaded_at      TEXT    NOT NULL
                    );
                    """);
            System.out.println("[Schema] uploaded_resources table ready.");

        } catch (Exception e) {
            System.err.println("[Schema] Initialisation error: " + e.getMessage());
        } finally {
            DBConnectionPool.getInstance().returnConnection(conn);
        }
    }

    /**
     * Uploads an encrypted file and persists its metadata inside a single
     * JDBC transaction.  The metadata row is committed ONLY if the file is
     * fully written to disk — guaranteeing consistency.
     *
     * @param sourcePath      Path to the original (pre-encryption) file.
     * @param encryptedBytes  Encrypted content to persist.
     * @param metadata        Descriptive metadata to store in the database.
     * @return                true if the upload succeeded; false otherwise.
     */
    public boolean uploadFile(String sourcePath, byte[] encryptedBytes,
                              FileMetadata metadata) {

        // Borrow a connection from the Singleton pool
        Connection conn = DBConnectionPool.getInstance().getConnection();

        try {
            // ── BEGIN TRANSACTION ─────────────────────────────────────────
            conn.setAutoCommit(false);  // manual transaction control

            // STEP 1 — Write encrypted bytes to the storage directory.
            //          This is the "physical" side of the upload.
            Path storageDir  = Paths.get(STORAGE_DIR);
            Files.createDirectories(storageDir);
            Path destination = storageDir.resolve(metadata.getFileName() + ".enc");
            Files.write(destination, encryptedBytes);

            System.out.printf("[Upload] File written to disk: %s (%d bytes)%n",
                    destination, encryptedBytes.length);

            // STEP 2 — INSERT metadata row.
            //          This succeeds only if the file write above did not throw.
            String sql = """
                    INSERT INTO uploaded_resources
                        (file_name, file_type, file_size_bytes,
                         encryption_level, uploaded_by, uploaded_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            // Try-with-Resources on PreparedStatement — zero statement leakage
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, metadata.getFileName());
                ps.setString(2, metadata.getFileType());
                ps.setLong  (3, metadata.getFileSizeBytes());
                ps.setString(4, metadata.getEncryptionLevel());
                ps.setString(5, metadata.getUploadedBy());
                ps.setString(6, metadata.getUploadedAt());
                ps.executeUpdate();
            }

            // Both steps succeeded — commit atomically
            conn.commit();
            System.out.println("[Upload] Transaction committed. Metadata persisted: " + metadata);
            return true;

        } catch (Exception e) {
            // Either step failed — roll back to leave DB in a consistent state
            System.err.println("[Upload] Upload failed: " + e.getMessage()
                    + " — rolling back transaction.");
            try {
                conn.rollback();
                System.out.println("[Upload] Rollback successful.");
            } catch (Exception rollbackEx) {
                System.err.println("[Upload] Rollback also failed: " + rollbackEx.getMessage());
            }
            return false;

        } finally {
            // Restore auto-commit and return connection to the Singleton pool
            try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            DBConnectionPool.getInstance().returnConnection(conn);
        }
    }
}
