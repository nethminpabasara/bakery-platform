package com.bakery.bakeryplatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentStatusMigrationTest {
    @Test void extendsOldEnumWithoutChangingExistingPaymentStatuses() {
        var dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:payment-enum-upgrade", "sa", "", true);
        try {
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE PAYMENTS (ID BIGINT PRIMARY KEY, STATUS ENUM('PAID','REFUNDED'))");
            jdbc.update("INSERT INTO PAYMENTS VALUES (1, 'PAID'), (2, 'REFUNDED')");
            var migration = new StatusMigration(jdbc);
            migration.migrate();
            assertDoesNotThrow(() -> jdbc.update("INSERT INTO PAYMENTS VALUES (3, 'PENDING')"));
            assertEquals("PAID", jdbc.queryForObject("SELECT STATUS FROM PAYMENTS WHERE ID=1", String.class));
            assertEquals("REFUNDED", jdbc.queryForObject("SELECT STATUS FROM PAYMENTS WHERE ID=2", String.class));
            assertDoesNotThrow(migration::migrate);
        } finally { dataSource.destroy(); }
    }
}
