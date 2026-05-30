package com.quizapp.dao;

import com.quizapp.util.ConfigLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ConnectionPool {
    private static HikariDataSource dataSource;

    static {
        try {
            // Load driver class explicitly
            String driver = ConfigLoader.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            Class.forName(driver);

            // Attempt to create database if it doesn't exist
            ensureDatabaseExists();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(ConfigLoader.getProperty("db.url", "jdbc:mysql://localhost:3306/quizdb?useSSL=false&allowPublicKeyRetrieval=true"));
            config.setUsername(ConfigLoader.getProperty("db.username", "root"));
            config.setPassword(ConfigLoader.getProperty("db.password", ""));
            config.setDriverClassName(driver);
            
            config.setMaximumPoolSize(ConfigLoader.getIntProperty("db.pool.maxSize", 10));
            config.setIdleTimeout(ConfigLoader.getIntProperty("db.pool.idleTimeout", 600000));
            config.setConnectionTimeout(ConfigLoader.getIntProperty("db.pool.connectionTimeout", 30000));
            
            // Helpful tuning defaults
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP connection pool initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fatal error: Failed to initialize Connection Pool!");
        }
    }

    private static void ensureDatabaseExists() {
        String dbUrl = ConfigLoader.getProperty("db.url", "jdbc:mysql://localhost:3306/quizdb");
        String username = ConfigLoader.getProperty("db.username", "root");
        String password = ConfigLoader.getProperty("db.password", "");

        if (!dbUrl.contains("jdbc:mysql://")) {
            // If it's H2 or SQLite, no need to boot database manually
            return;
        }

        // Extract server URL (e.g. jdbc:mysql://localhost:3306/)
        String serverUrl = dbUrl.substring(0, dbUrl.indexOf("?", dbUrl.indexOf("3306/")));
        if (serverUrl.endsWith("/quizdb")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 7);
        }

        System.out.println("Checking / creating database quizdb if not exists using connection: " + serverUrl);
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS quizdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("Database 'quizdb' verified / created successfully.");
        } catch (Exception e) {
            System.err.println("Warning: Failed to ensure database 'quizdb' exists. Connection pool might fail on startup: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws Exception {
        if (dataSource == null) {
            throw new IllegalStateException("Data source is not initialized properly.");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("HikariCP connection pool shut down successfully.");
        }
    }
}
