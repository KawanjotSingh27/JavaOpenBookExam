package com.portal.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// ──────────────────────────────────────────────────────────────────────────────
// PDFParser — Concrete product for .pdf files
// ──────────────────────────────────────────────────────────────────────────────

/**
 * PDFParser simulates extraction of text from a PDF document.
 *
 * In a production FOSS environment this would delegate to Apache PDFBox
 * (open-source, Apache-2.0), but for this exam we demonstrate the interface
 * contract without an external binary dependency.
 */
class PDFParser implements FileParser {

    @Override
    public String parse(String filePath) {
        System.out.println("[PDFParser] Parsing PDF: " + filePath);
        // In production: use org.apache.pdfbox.pdmodel.PDDocument
        // PDDocument doc = PDDocument.load(new File(filePath)); ...
        try {
            // Read raw bytes and return a UTF-8 best-effort extraction
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return "[PDF Content Extracted] Size: " + bytes.length + " bytes from " + filePath;
        } catch (IOException e) {
            return "[PDFParser] Error reading file: " + e.getMessage();
        }
    }

    @Override
    public String getSupportedFormat() {
        return "application/pdf";
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// MarkdownParser — Concrete product for .md files
// ──────────────────────────────────────────────────────────────────────────────

/**
 * MarkdownParser reads a Markdown file and strips common syntax tokens
 * (headers, bold, italic, code fences) to yield clean plain text.
 */
class MarkdownParser implements FileParser {

    @Override
    public String parse(String filePath) {
        System.out.println("[MarkdownParser] Parsing Markdown: " + filePath);
        try {
            String raw = Files.readString(Paths.get(filePath));
            // Strip Markdown syntax for plain-text extraction
            String plain = raw
                    .replaceAll("#{1,6}\\s*", "")     // headings
                    .replaceAll("\\*{1,2}(.*?)\\*{1,2}", "$1")  // bold/italic
                    .replaceAll("`{1,3}[^`]*`{1,3}", "")        // inline code / fences
                    .replaceAll("!?\\[.*?]\\(.*?\\)", "")        // links & images
                    .trim();
            return plain;
        } catch (IOException e) {
            return "[MarkdownParser] Error reading file: " + e.getMessage();
        }
    }

    @Override
    public String getSupportedFormat() {
        return "text/markdown";
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// TextParser — Concrete product for .txt files
// ──────────────────────────────────────────────────────────────────────────────

/**
 * TextParser simply reads a plain-text file as-is.
 */
class TextParser implements FileParser {

    @Override
    public String parse(String filePath) {
        System.out.println("[TextParser] Parsing Text: " + filePath);
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            return "[TextParser] Error reading file: " + e.getMessage();
        }
    }

    @Override
    public String getSupportedFormat() {
        return "text/plain";
    }
}
