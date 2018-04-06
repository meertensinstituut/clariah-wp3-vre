package nl.knaw.meertens.clariah.vre.integration.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ObjectsRepositoryService {

    private String user;
    private String password;
    private String database;

    public ObjectsRepositoryService(String database, String user, String password) {
        this.user = user;
        this.password = password;
        this.database = database;
    }

    public void processQuery(String query, ExceptionalConsumer<ResultSet> function) throws SQLException {
        String url = String.format("jdbc:postgresql://postgres:5432/%s?user=%s&password=%s", database, user, password);
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)
        ) {
            function.accept(resultSet);
        }
    }

}
