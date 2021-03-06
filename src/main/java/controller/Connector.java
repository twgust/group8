package controller;

import java.sql.*;

/**
 * controller.Connector class, handles the connection to the SQL database and functions used to query data from the DB
 */
public class Connector
{
    private Connection connection;

    /**
     * Attempts to establish connection to the SQL server
     */
    public Connector()
    {
        try {
            System.out.println("Connecting to FoodBank database...");
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;database=FoodBankDB;", "user", "password");
            System.out.println("Successfully connected");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the connected instance to the DB
     * @return The connection to the DB
     */
    public Connection getConnection()
    {
        return this.connection;
    }

}
