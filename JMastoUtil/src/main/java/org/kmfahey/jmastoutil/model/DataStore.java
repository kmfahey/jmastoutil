package org.kmfahey.jmastoutil.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataStore {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            System.err.println("Unable to load the SQLite JDBC component:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private static final String appDirName = ".jmastoutil";
    private static final String databaseName = "mastoAcctData.db";
    private static final Set<String> databaseTables = new HashSet<>(Arrays.asList("profiles", "profiles_fts", "notifs", "follow"));
    private Connection dbConnection = null;

    public DataStore() {
        Path sqliteFilePath = getDatabasePath();
        try {
            this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFilePath);
        } catch (SQLException exception) {
            System.err.println("Unable to open SQLite3 database file at " + sqliteFilePath + " due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
        Set<String> tablesInDatabase = checkTablesInDatabase();
        switch (tablesInDatabase.size()) {
            case 0 -> { /* database has no tables, therefore it's new */ }
            case 4 -> { /* database has all expected tables, therefore it's pre-existing and likely nonempty */ }
            default -> { /* database has some tables not others, got corrupted somehow */ }
        }
    }

    private Set<String> checkTablesInDatabase() {
        Set<String> tablesFound = new HashSet<>();
        try (
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")
        ) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("name");
                if (databaseTables.contains(tableName)) {
                    tablesFound.add(tableName);
                }
            }
        } catch (SQLException exception) {
            System.err.println("Unable to load list of tables in database from sqlite_master table due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
        return tablesFound;
    }

    /* Got the below code from ChatGPT. Needs rewriting to store relevant vars as instance fields, and to do the work
       based out of the constructor */

    private Path getDatabasePath() {
        String userHome = System.getProperty("user.home");

        Path appDirPath = Paths.get(userHome, appDirName);
        if (Files.notExists(appDirPath)) {
            try {
                Files.createDirectories(appDirPath); // Create the directory if it doesn't exist
            } catch (IOException exception) {
                System.err.println("Unable to create .jmastoutil directory under user's home directory:");
                exception.printStackTrace();
                System.exit(1);
            }
        }

        return appDirPath.resolve(databaseName);
    }
}
