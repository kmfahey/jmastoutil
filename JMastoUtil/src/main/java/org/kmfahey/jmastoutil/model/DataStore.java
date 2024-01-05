package org.kmfahey.jmastoutil.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
    }

    /* Got the below code from ChatGPT. Needs rewriting to store relevant vars as instance fields, and to do the work
       based out of the constructor */

    public static Path getDatabasePath() {
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
