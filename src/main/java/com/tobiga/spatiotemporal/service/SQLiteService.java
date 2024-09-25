package com.tobiga.spatiotemporal.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SQLiteService {

    private final JdbcTemplate jdbcTemplate;

    public SQLiteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initialize() {
        try {
            // Create a table
            String createTableSql = "CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT)";
            jdbcTemplate.execute(createTableSql);
            System.out.println("Table created successfully.");

            // Insert a row
            String insertSql = "INSERT INTO test_table (name) VALUES ('Sample Name')";
            jdbcTemplate.update(insertSql);
            System.out.println("Data inserted successfully.");

            // Query the data
            String querySql = "SELECT * FROM test_table";
            jdbcTemplate.query(querySql, (rs, rowNum) -> {
                System.out.println("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
                return null;
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

