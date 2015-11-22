/**
 * This application uses a Derby database connection.
 * 
 * The database handler will handle everything database
 * related, such as creating necessary tables, adding,
 * editing and removing data from the database.
 */
package com.ludo.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Petter
 *
 */
public class DatabaseHandler {
    
    /**
     * Database Connection
     */
    private static Connection connection;
    
    /**
     * Database Driver
     */
    private final static String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    
    /**
     * Database Server URL
     */
    private final static String derbyURL = "jdbc:derby:ludo;create=true";
    
    /**
     * DatabaseHandler constructor to construct the necessary
     * database tables.
     */
    public DatabaseHandler() {
        
        // Create and remove tables (Testing)
        createTables();
        dropTables();
        
    }
    
    /**
     * Create a connection to the database
     * @return boolean
     */
    private boolean createConnection() {
        
        // Try connecting
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(derbyURL);
            
        } catch (Exception e) {
            System.out.println("Error connecting to database: " + e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Reset all tables by clearing their data.
     */
    public void resetTables() {
        
        // List of queries
        List<String> queries = new ArrayList<String>();
        
        // Truncate tables
        queries.add("TRUNCATE TABLE users");
        
        execute(queries);
    }
    
    /**
     * Create necessary Database Tables
     */
    private void createTables() {
        
        // List of queries
        List<String> queries = new ArrayList<String>();
        
        // Create users table
        queries.add("CREATE TABLE users (id integer primary key,"
                                      + "username VARCHAR(50) UNIQUE,"
                                      + "password VARCHAR(50))");
        
        // Execute the queries
        execute(queries);
    }
    
    /**
     * Drop all tables
     */
    private void dropTables() {
        
        // List of queries
        List<String> queries = new ArrayList<String>();
        
        // Drop users table
        queries.add("DROP TABLE users");
        
        // Execute the queries
        execute(queries);
    }
    
    /**
     * Execute a list of queries
     * @param queries
     */
    private void execute(List<String> queries) {
        
        // Check if a database connection can be created
        if(createConnection()) {
            
            // Try executing the queries
            try {
                
                Statement stmt = connection.createStatement();
                
                // For each queries and query...
                for (String query : queries) {
                    
                    // Execute query
                    stmt.execute(query);
                }
                
                // Close database connection
                connection.close();
                
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
            
        }
        
    }
}
