package nl.knaw.meertens.clariah.vre.integration.util;

import com.mashape.unirest.http.exceptions.UnirestException;

import java.sql.SQLException;

/**
 *
 */
@FunctionalInterface
public interface Procedure {
    void execute() throws AssertionError;
}