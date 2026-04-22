# Advanced Java Programming — Open Book Exam
### Course Code: CIE-306T | Session: Jan–June 2026

---

## Project Structure

```
Q1_Weather/
├── WeatherBuffer.java      → Thread-safe buffer (ReentrantLock + Condition)
├── WeatherStation.java     → Producer thread (15 instances)
├── AlertConsumer.java      → Consumer thread (writes alerts to SQLite)
├── WeatherDatabase.java    → JDBC + Try-with-Resources SQLite wrapper
└── WeatherMain.java        → Entry point / orchestrator

Q2_Portal/
├── parser/
│   ├── FileParser.java           → Product interface (Factory Pattern)
│   ├── ConcreteFileParsers.java  → PDFParser, MarkdownParser, TextParser
│   └── ParserFactory.java        → Factory Method — selects parser by extension
├── encryption/
│   └── EncryptionStrategy.java   → Strategy interface + NoEncryption,
│                                    AES128Encryption, AES256Encryption, EncryptionFactory
├── di/
│   └── ReflectionLoader.java     → Manual DI via Java Reflection + URLClassLoader
├── db/
│   ├── DBConnectionPool.java     → Singleton connection pool (double-checked lock)
│   ├── FileMetadata.java         → Immutable value object
│   └── FileUploadService.java    → JDBC transaction (setAutoCommit(false))
└── PortalMain.java               → Entry point demonstrating all patterns
```

---

## Question 1 — High-Frequency Data Ingestion System
### Bloom's Level: Create (L6)

Designed from scratch — not adapted from a template. Architecture uses bounded producer-consumer with ReentrantLock + Condition over simpler alternatives (e.g., BlockingQueue) to demonstrate explicit lock management.

### Thread Safety Strategy
- `ReentrantLock(true)` — fair lock prevents starvation among 15 producers.
- Two `Condition` objects (`notFull`, `notEmpty`) replace `wait()/notifyAll()`, eliminating spurious wake-ups.
- `AtomicBoolean` controls consumer shutdown without synchronization overhead.
- `ExecutorService` (fixed pool) manages producer lifecycle cleanly.

### Green Computing / SDG-9
- Buffer is **bounded** (50 slots) to cap memory under burst load.
- **Try-with-Resources** on every `Connection` and `Statement` ensures OS-level file descriptors are freed immediately.
- SQLite (FOSS, file-based) requires no separate server process.

---

## Question 2 — Modular Student Resource Portal
### Bloom's Levels: Evaluate (L5) + Create (L6)

### Design Patterns Used

| Pattern      | Class(es)                          | Justification |
|--------------|------------------------------------|---------------|
| Factory      | `ParserFactory`                    | Decouples caller from concrete parsers; new types need one new class + one line |
| Strategy     | `EncryptionStrategy` + impls       | Swaps algorithm at runtime without touching upload logic |
| Singleton    | `DBConnectionPool`                 | One pool per JVM → memory-efficient, protects DB connection limit |
| Value Object | `FileMetadata`                     | Immutable DTO between service and persistence layers |

### Manual DI (Java Reflection)
`ReflectionLoader.loadParser()` uses URLClassLoader + Class.forName() + Constructor.newInstance() — mirrors Spring's ApplicationContext, zero external libraries.

### JDBC Transaction Atomicity
`setAutoCommit(false)` wraps file-write + DB-INSERT in one atomic unit.
`conn.commit()` fires only when BOTH steps succeed.
`conn.rollback()` undoes partial state on any failure.

### Singleton Justification
A database connection is expensive (network handshake, server-side thread). One Singleton pool bounded to MAX_SIZE=5 connections caps heap usage and prevents DB lock exhaustion — directly supporting SDG-9 resource-efficient infrastructure.

---

## FOSS Compliance
| Dependency  | License    | Note |
|-------------|------------|------|
| OpenJDK 17+ | GPL-2.0-CE | Java 17+ for text blocks & switch expressions |
| SQLite JDBC | Apache-2.0 | org.xerial:sqlite-jdbc:3.x |

---

## Compile & Run

```bash
# Compile
javac -cp sqlite-jdbc-3.45.3.0.jar Q1_Weather/*.java Q2_Portal/**/*.java

# Run Q1
java -cp .:sqlite-jdbc-3.45.3.0.jar com.weather.WeatherMain

# Run Q2
java -cp .:sqlite-jdbc-3.45.3.0.jar com.portal.PortalMain
```

*Submitted by: AJ | Section: 6AIML-VI(123) | Session: Jan–June 2026*
