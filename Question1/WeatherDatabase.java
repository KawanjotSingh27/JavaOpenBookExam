package com.weather;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * WeatherDatabase — handles all SQLite persistence for extreme-weather alerts.
 *
 * GREEN COMPUTING NOTE:
 *   Every method uses Try-with-Resources (TWR) on both Connection and
 *   PreparedStatement objects.  TWR guarantees that even if an exception is
 *   thrown mid-method, the JDBC resources are closed immediately — no lingering
 *   file handles or open connections are left on the operating-system layer,
 *   which aligns with the SDG-9 mandate for resource-efficient infrastructure.
 *
 * DATABASE: SQLite (file-based, FOSS, zero external server dependency).
 * DRIVER:   org.xerial:sqlite-jdbc (open-source, Apache-2 licensed).
 */
public class WeatherDatabase {

    private static final String DB_URL = "jdbc:sqlite:weather_alerts.db";

    /**
     * Initialises the SQLite database and creates the alerts table if it does
     * not already exist.  Called once at application startup.
     */
    public static void initialise() {
        // TWR closes both Connection and Statement automatically
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement  stmt = conn.createStatement()) {

            String ddl = """
                    CREATE TABLE IF NOT EXISTS extreme_weather_alerts (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        temperature REAL        NOT NULL,
                        alert_time  TEXT        NOT NULL,
                        message     TEXT        NOT NULL
                    );
                    """;
            stmt.execute(ddl);
            System.out.println("[DB] Database initialised: weather_alerts.db");

        } catch (SQLException e) {
            System.err.println("[DB] Initialisation failed: " + e.getMessage());
        }
    }

    /**
     * Persists a single extreme-weather alert record.
     *
     * @param temperature The temperature reading that triggered the alert (> 45°C).
     */
    public static void saveAlert(double temperature) {
        String sql = "INSERT INTO extreme_weather_alerts (temperature, alert_time, message) VALUES (?, ?, ?)";

        // TWR closes Connection and PreparedStatement automatically —
        // zero resource leakage even if an exception propagates.
        try (Connection       conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps   = conn.prepareStatement(sql)) {

            ps.setDouble(1, temperature);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, String.format("EXTREME WEATHER ALERT: %.2f°C detected!", temperature));
            ps.executeUpdate();

            System.out.printf("[DB] Alert saved → %.2f°C at %s%n",
                    temperature, LocalDateTime.now());

        } catch (SQLException e) {
            System.err.println("[DB] Failed to save alert: " + e.getMessage());
        }
    }
}
