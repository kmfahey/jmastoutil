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
        int tablesCount = countTablesInDatabase();
        if (tablesCount == 0) {
            /* The database has no tables, therefore it's new. */
            createTablesAndIndex();
        } else if (tablesCount != 4) {
            /* The correct number of tables is 4. So database has some tables not others; it got corrupted somehow. */
            dropAllTablesAndIndex();
            createTablesAndIndex();
        }
    }

    private void createTablesAndIndex() {
        createProfilesTable();
        /* This index on the profiles needs to be created after the table is created by the above method call. */
        createProfilesIndex();
        createProfilesFtsTable();
        createNotifsTable();
        createFollowTable();
    }

    private int countTablesInDatabase() {
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
        return tablesFound.size();
    }

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

    private void enableForeignKeys() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException exception) {
            System.err.println("Unable to enable foreign keys due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private void createProfilesTable() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("""
                CREATE TABLE profiles (
                    user_id TEXT PRIMARY KEY NOT NULL,
                    fts_rowid INT,
                    acct_id INT NOT NULL,
                    user_name TEXT NOT NULL,
                    instance TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    field_name_1 TEXT,
                    field_value_1 TEXT,
                    field_name_2 TEXT,
                    field_value_2 TEXT,
                    field_name_3 TEXT,
                    field_value_3 TEXT,
                    field_name_4 TEXT,
                    field_value_4 TEXT,
                    profile_text TEXT NOT NULL,
                    earliest_notif DATETIME NOT NULL,
                    loginable BOOLEAN NOT NULL,
                    tested BOOLEAN NOT NULL
                );
            """);
        } catch (SQLException exception) {
            System.err.println("Unable to create profiles table due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private void createProfilesIndex() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE INDEX idx_profiles_fts_rowid ON profiles (fts_rowid);");
        } catch (SQLException exception) {
            System.err.println("Unable to add index to profiles table for `fts_rowid` column due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private void createProfilesFtsTable() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("""
                CREATE VIRTUAL TABLE profiles_fts USING fts5(
                    user_id,
                    user_name,
                    instance,
                    field_name_1,
                    field_value_1,
                    field_name_2,
                    field_value_2,
                    field_name_3,
                    field_value_3,
                    field_name_4,
                    field_value_4,
                    profile_text
                );
            """);
        } catch (SQLException exception) {
            System.err.println("Unable to create virtual table profiles_fts due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private void createNotifsTable() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("""
                CREATE TABLE notifs (
                    from_user_id TEXT PRIMARY KEY NOT NULL,
                    to_user_id TEXT NOT NULL,
                    created_at DATETIME NOT NULL,
                    notif_type TEXT NOT NULL,
                    status_uri TEXT,
                    FOREIGN KEY (from_user_id) REFERENCES profiles(user_id),
                    FOREIGN KEY (to_user_id) REFERENCES profiles(user_id)
                );
            """);
        } catch (SQLException exception) {
            System.err.println("Unable to create notifs table due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }

    }

    private void createFollowTable() {
        try (Statement statement = dbConnection.createStatement()) {
            statement.execute("""
                CREATE TABLE follow (
                    by_user_id TEXT PRIMARY KEY NOT NULL,
                    of_user_id TEXT NOT NULL,
                    last_event DATETIME NOT NULL,
                    relation_type TEXT NOT NULL,
                    FOREIGN KEY (by_user_id) REFERENCES profiles(user_id),
                    FOREIGN KEY (of_user_id) REFERENCES profile(user_id)
                );
            """);
        } catch (SQLException exception) {
            System.err.println("Unable to create follow table due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }

    private int getLastInsertRowid() {
        try (
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT last_insert_rowid();")
        ) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException exception) {
            System.err.println("Unable to fetch rowid from last insert into profiles_fts due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    private boolean updateProfilesTableWithRowid(String userId) {
        try (Statement statement = dbConnection.createStatement()) {
            int rowsAffected = statement.executeUpdate("""
                UPDATE profiles
                SET fts_rowid = (
                    SELECT rowid
                    FROM profiles_fts
                    WHERE profiles_fts.user_id = "%s"
                )
                WHERE user_id = "%s";
            """.formatted(userId, userId));
            return rowsAffected > 0;
        } catch (SQLException exception) {
            System.err.println(
                    "Unable to update profiles table with rowid from profiles_fts table "
                    + "for user_id '%s' due to SQL error:".format(userId)
            );
            exception.printStackTrace();
            System.exit(1);
        }
        return false;
    }

    private void dropAllTablesAndIndex() {
        try (Statement statement = dbConnection.createStatement()) {
            /* Dropping the tables and index in the reverse order from how they're meant to be created. */
            statement.execute("DROP TABLE IF EXISTS follow;");
            statement.execute("DROP TABLE IF EXISTS notifs;");
            statement.execute("DROP TABLE IF EXISTS profiles_fts;");
            statement.execute("DROP INDEX IF EXISTS idx_profiles_fts_rowid;");
            statement.execute("DROP TABLE IF EXISTS profiles;");
        } catch (SQLException exception) {
            System.err.println("Unable to drop tables and indexes due to SQL error:");
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
