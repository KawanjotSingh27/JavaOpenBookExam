package com.portal.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * DBConnectionPool — Singleton Pattern implementation for JDBC connection pooling.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  WHY SINGLETON IS THE RIGHT PATTERN FOR A DB CONNECTION POOL            │
 * │                                                                          │
 * │  A database connection is an expensive OS-level resource (network       │
 * │  socket + server-side thread).  If each service or DAO class created    │
 * │  its own pool, the application would:                                   │
 * │    • Waste heap memory holding duplicate Connection objects.            │
 * │    • Risk exceeding the DB server's max-connections limit.              │
 * │    • Lose the throughput benefit of connection reuse.                   │
 * │                                                                          │
 * │  The Singleton pattern guarantees exactly ONE pool exists per JVM,      │
 * │  shared by all callers.  Combined with a bounded pool size (MAX_SIZE),  │
 * │  it caps memory use and protects the DB server — directly supporting   │
 * │  SDG-9 resource-efficient infrastructure.                               │
 * │                                                                          │
 * │  Thread safety is achieved via volatile + double-checked locking,      │
 * │  which is the standard Java idiom for lazy-initialised singletons.     │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
public class DBConnectionPool {

    private static final String DB_URL  = "jdbc:sqlite:portal_resources.db";
    private static final int    MAX_SIZE = 5;   // max pooled connections

    // volatile guarantees visibility of the reference across threads
    private static volatile DBConnectionPool INSTANCE;

    private final Deque<Connection> pool = new ArrayDeque<>();

    // ── Private constructor — Singleton enforcement ──────────────────────────
    private DBConnectionPool() {
        try {
            for (int i = 0; i < MAX_SIZE; i++) {
                pool.push(DriverManager.getConnection(DB_URL));
            }
            System.out.println("[DBPool] Initialised with " + MAX_SIZE + " connections.");
        } catch (SQLException e) {
            throw new RuntimeException("DBConnectionPool init failed: " + e.getMessage(), e);
        }
    }

    // ── Double-checked locking — thread-safe lazy initialisation ────────────
    /**
     * Returns the single application-wide connection pool instance.
     * Creates it on first call (lazy); subsequent calls return the cached instance.
     */
    public static DBConnectionPool getInstance() {
        if (INSTANCE == null) {                      // first check (no lock)
            synchronized (DBConnectionPool.class) {
                if (INSTANCE == null) {              // second check (with lock)
                    INSTANCE = new DBConnectionPool();
                }
            }
        }
        return INSTANCE;
    }

    // ── Pool borrow / return ─────────────────────────────────────────────────

    /**
     * Borrows a connection from the pool.
     * Callers MUST return it via {@link #returnConnection(Connection)}.
     *
     * @return A ready-to-use JDBC Connection.
     * @throws RuntimeException if the pool is exhausted.
     */
    public synchronized Connection getConnection() {
        if (pool.isEmpty()) {
            throw new RuntimeException(
                    "Connection pool exhausted. Maximum active connections: " + MAX_SIZE);
        }
        return pool.pop();
    }

    /**
     * Returns a previously borrowed connection back to the pool for reuse.
     *
     * @param conn The connection to return.
     */
    public synchronized void returnConnection(Connection conn) {
        if (conn != null) {
            pool.push(conn);
        }
    }

    /**
     * Closes all pooled connections at application shutdown.
     * Call from a JVM shutdown hook or a ServletContextListener.
     */
    public synchronized void closeAll() {
        for (Connection conn : pool) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
        pool.clear();
        System.out.println("[DBPool] All connections closed.");
    }
}
