package com.example.gastroreservabackend1;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GastroreservaBackend1ApplicationTests {

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoads() {
    }

    @Test
    void appliesAllDatabaseMigrations() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
    }

}
