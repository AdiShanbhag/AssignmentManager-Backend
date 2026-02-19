package com.applicationplanner.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayShouldCreateTables() throws Exception {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData()
                     .getTables(null, null, "ASSIGNMENTS", null)) {

            assertTrue(rs.next(), "ASSIGNMENTS table should exist");
        }
    }
}