package com.subscription;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens a connection to the MySQL database.
 * Settings come from environment variables, with safe defaults for a local setup.
 */
public class DBConnection {

    private static final String URL = getEnv("DB_URL",
            "jdbc:mysql://localhost:3306/subscription_manager");
    private static final String USER = getEnv("DB_USER", "root");
    private static final String PASSWORD = getEnv("DB_PASSWORD", "");

    private DBConnection() {
        // Utility class: no objects needed, only the static method.
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
