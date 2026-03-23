package com.goaiplatform.backend.repository;

import com.goaiplatform.backend.domain.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataR2dbcTest
@Testcontainers
class UserRepositoryTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("go_ai")
            .withUsername("goai")
            .withPassword("goai123")
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:mysql://%s:%d/%s",
                MYSQL.getHost(),
                MYSQL.getFirstMappedPort(),
                MYSQL.getDatabaseName()
        ));
        registry.add("spring.r2dbc.username", MYSQL::getUsername);
        registry.add("spring.r2dbc.password", MYSQL::getPassword);
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameShouldPopulatePasswordHash() {
        UserEntity user = new UserEntity(null, "alice", "encoded-password", "USER", LocalDateTime.now());

        StepVerifier.create(userRepository.save(user).then(userRepository.findByUsername("alice")))
                .assertNext(saved -> {
                    assertEquals("alice", saved.username());
                    assertEquals("encoded-password", saved.passwordHash());
                    assertEquals("USER", saved.role());
                })
                .verifyComplete();
    }
}
