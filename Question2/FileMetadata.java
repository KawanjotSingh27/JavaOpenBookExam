package com.portal.db;

/**
 * FileMetadata — plain data object representing a single uploaded study resource.
 *
 * This is a simple Value Object (immutable after construction) used to carry
 * upload metadata between the service layer and the JDBC persistence layer.
 * Keeping it free of any framework annotations adheres to the FOSS constraint.
 */
public class FileMetadata {

    private final String fileName;
    private final String fileType;          // e.g. "pdf", "md", "txt"
    private final long   fileSizeBytes;
    private final String encryptionLevel;   // e.g. "NONE", "AES-128", "AES-256"
    private final String uploadedBy;        // faculty username
    private final String uploadedAt;        // ISO-8601 timestamp

    public FileMetadata(String fileName, String fileType, long fileSizeBytes,
                        String encryptionLevel, String uploadedBy, String uploadedAt) {
        this.fileName        = fileName;
        this.fileType        = fileType;
        this.fileSizeBytes   = fileSizeBytes;
        this.encryptionLevel = encryptionLevel;
        this.uploadedBy      = uploadedBy;
        this.uploadedAt      = uploadedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getFileName()        { return fileName; }
    public String getFileType()        { return fileType; }
    public long   getFileSizeBytes()   { return fileSizeBytes; }
    public String getEncryptionLevel() { return encryptionLevel; }
    public String getUploadedBy()      { return uploadedBy; }
    public String getUploadedAt()      { return uploadedAt; }

    @Override
    public String toString() {
        return String.format("FileMetadata{name='%s', type='%s', size=%d bytes, "
                + "encryption='%s', by='%s', at='%s'}",
                fileName, fileType, fileSizeBytes,
                encryptionLevel, uploadedBy, uploadedAt);
    }
}
