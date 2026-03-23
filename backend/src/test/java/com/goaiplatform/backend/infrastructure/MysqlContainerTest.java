package com.goaiplatform.backend.infrastructure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MysqlContainerTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("go_ai")
            .withUsername("goai")
            .withPassword("goai123");

    @Test
    void mysqlContainerShouldStart() {
        Assertions.assertTrue(MYSQL.isRunning());
    }
}
