package com.portal.parser;

/**
 * ParserFactory — Factory Method Pattern implementation.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  FACTORY PATTERN JUSTIFICATION                                           │
 * │                                                                          │
 * │  The portal's upload logic only ever calls ParserFactory.getParser().   │
 * │  It never constructs a PDFParser, MarkdownParser, or TextParser          │
 * │  directly.  This single point of creation means:                         │
 * │                                                                          │
 * │  1. OPEN/CLOSED PRINCIPLE — adding DocxParser requires one new class     │
 * │     + one new case in this factory; the upload pipeline is untouched.   │
 * │  2. TESTABILITY — unit tests can inject a mock parser by sub-classing    │
 * │     this factory and overriding getParser().                             │
 * │  3. SINGLE RESPONSIBILITY — file-type decision logic lives here only.   │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * PLUGGABILITY: To add a new parser type (e.g., .csv):
 *   1. Create CsvParser implements FileParser  (no other file changes)
 *   2. Add case "csv" -> new CsvParser();  below.
 *   Done — the rest of the portal is unchanged.
 */
public class ParserFactory {

    /**
     * Returns the appropriate FileParser for the given file extension.
     *
     * @param fileExtension Lowercase file extension without the dot
     *                      (e.g. "pdf", "md", "txt").
     * @return Concrete FileParser instance.
     * @throws IllegalArgumentException if the extension is unsupported.
     */
    public static FileParser getParser(String fileExtension) {
        if (fileExtension == null) {
            throw new IllegalArgumentException("File extension must not be null.");
        }

        // Java 14+ switch expression — concise and exhaustive
        return switch (fileExtension.toLowerCase().trim()) {
            case "pdf"       -> new PDFParser();
            case "md", "markdown" -> new MarkdownParser();
            case "txt"       -> new TextParser();
            default          -> throw new IllegalArgumentException(
                    "No parser registered for extension: '" + fileExtension + "'. "
                    + "Add a new ConcreteParser class and register it here.");
        };
    }

    /**
     * Convenience overload: derives the extension from a full filename.
     *
     * @param fileName Full filename, e.g. "lecture_notes.md".
     * @return Concrete FileParser instance.
     */
    public static FileParser getParserForFile(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Cannot determine extension from: " + fileName);
        }
        String ext = fileName.substring(dotIndex + 1);
        return getParser(ext);
    }
}
