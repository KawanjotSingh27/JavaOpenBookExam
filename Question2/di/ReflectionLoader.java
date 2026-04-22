package com.portal.di;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * ReflectionLoader — Manual Dependency Injection via Java Reflection.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  MANUAL DI DESIGN                                                        │
 * │                                                                          │
 * │  The portal must support "pluggable" parsers added by faculty admins    │
 * │  without recompiling the core system.  ReflectionLoader fulfils this    │
 * │  by:                                                                     │
 * │    1. Scanning a designated "parsers" folder for .class / .jar files.   │
 * │    2. Using URLClassLoader to load any class found at runtime.           │
 * │    3. Using Class.forName() + newInstance() to instantiate the parser.  │
 * │    4. Returning it as a FileParser interface — the caller is decoupled  │
 * │       from the concrete implementation entirely.                         │
 * │                                                                          │
 * │  This is the same principle used by heavyweight DI frameworks            │
 * │  (Spring, Guice) but implemented manually as the exam requires.         │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * FOSS NOTE: Only standard OpenJDK APIs are used (java.lang.reflect,
 * java.net.URLClassLoader) — no Spring, Guice, or CDI dependency.
 */
public class ReflectionLoader {

    /**
     * Registry mapping file extensions to the fully-qualified class name of
     * their parser.  In production this would be loaded from a parsers.properties
     * file in the plugins folder, making it fully config-driven.
     */
    private static final Map<String, String> EXTENSION_TO_CLASS = new HashMap<>();

    static {
        // Default built-in registrations (no reflection needed for these)
        EXTENSION_TO_CLASS.put("pdf",      "com.portal.parser.PDFParser");
        EXTENSION_TO_CLASS.put("md",       "com.portal.parser.MarkdownParser");
        EXTENSION_TO_CLASS.put("txt",      "com.portal.parser.TextParser");
    }

    /**
     * Dynamically registers a new parser class for a given extension.
     * Faculty admins call this (via a config file or admin UI) to add
     * support for new file types without touching core code.
     *
     * @param extension       File extension, e.g. "docx".
     * @param fullyQualifiedClassName e.g. "com.portal.parser.DocxParser".
     */
    public static void registerParser(String extension, String fullyQualifiedClassName) {
        EXTENSION_TO_CLASS.put(extension.toLowerCase(), fullyQualifiedClassName);
        System.out.println("[DI] Registered: ." + extension
                + " → " + fullyQualifiedClassName);
    }

    /**
     * Loads and instantiates a FileParser for the given file extension using
     * Java Reflection.  The plugin JAR/class directory is loaded via
     * URLClassLoader so that runtime-added parsers are found correctly.
     *
     * @param extension   File extension (e.g. "pdf", "md", "txt").
     * @param pluginDir   Directory that may contain external parser classes/jars.
     * @return            A fresh FileParser instance.
     * @throws ReflectiveOperationException if the class cannot be found or
     *                                       instantiated.
     */
    public static Object loadParser(String extension, String pluginDir)
            throws Exception {

        String className = EXTENSION_TO_CLASS.get(extension.toLowerCase());
        if (className == null) {
            throw new IllegalArgumentException(
                    "No parser registered for extension: ." + extension);
        }

        // Build a URLClassLoader that includes both the current classpath
        // and the external plugin directory (enabling hot-plugging).
        File dir = new File(pluginDir);
        URL[] urls;
        if (dir.exists() && dir.isDirectory()) {
            // Add each .jar in the plugin directory to the classpath
            File[] jars = dir.listFiles(f -> f.getName().endsWith(".jar"));
            urls = new URL[jars != null ? jars.length + 1 : 1];
            urls[0] = dir.toURI().toURL();
            if (jars != null) {
                for (int i = 0; i < jars.length; i++) {
                    urls[i + 1] = jars[i].toURI().toURL();
                }
            }
        } else {
            // Plugin directory absent — rely on application classpath only
            urls = new URL[]{};
        }

        // URLClassLoader delegates to parent (app classloader) first,
        // then checks the plugin directory — standard parent-delegation model.
        URLClassLoader pluginLoader = new URLClassLoader(
                urls, ReflectionLoader.class.getClassLoader());

        // ── REFLECTION CORE ──────────────────────────────────────────────
        // 1. Find the class by name at runtime (no compile-time dependency)
        Class<?> parserClass = Class.forName(className, true, pluginLoader);

        // 2. Locate the no-arg constructor via reflection
        java.lang.reflect.Constructor<?> ctor = parserClass.getDeclaredConstructor();
        ctor.setAccessible(true); // allow package-private constructors

        // 3. Instantiate — equivalent to "new PDFParser()" but decided at runtime
        Object parserInstance = ctor.newInstance();
        // ────────────────────────────────────────────────────────────────

        System.out.printf("[DI] Loaded via Reflection: %s for .%s%n",
                parserInstance.getClass().getSimpleName(), extension);

        return parserInstance;
    }
}
