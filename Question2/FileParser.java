package com.portal.parser;

/**
 * FileParser — the common contract for all file-type parsers.
 *
 * FACTORY PATTERN NOTE:
 *   This interface is the "Product" in the Factory Method pattern.
 *   The ParserFactory decides at runtime which concrete implementation
 *   (PDFParser, MarkdownParser, TextParser) to instantiate based on the
 *   uploaded file's extension — without the caller ever referencing a
 *   concrete class.  This makes the system "pluggable": adding a new
 *   parser (e.g., DocxParser) requires only a new class + one line in
 *   ParserFactory, with zero changes to the portal's core upload logic.
 */
public interface FileParser {

    /**
     * Reads and extracts plain text content from the given file path.
     *
     * @param filePath Absolute or relative path to the source file.
     * @return Extracted plain-text representation of the file's content.
     */
    String parse(String filePath);

    /**
     * Returns the MIME type this parser handles (for logging / validation).
     *
     * @return A human-readable format descriptor, e.g. "application/pdf".
     */
    String getSupportedFormat();
}
