package com.adonkov.reminders;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real Postgres for anything that leans on Postgres-specific behaviour --
 * {@code skip locked}, partial indexes, {@code lower()} unique indexes.
 * H2 would quietly do the wrong thing for all three.
 */
@Testcontainers
public abstract class AbstractPostgresTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
