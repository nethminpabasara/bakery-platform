package com.bakery.bakeryplatform.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class InvoiceDiscriminatorMigrationTest {

    @Test
    void addsPaymentToExistingInvoiceDiscriminatorConstraint() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:invoice-migration-test;DB_CLOSE_DELAY=-1", "sa", "", true);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("""
                    CREATE TABLE INVOICES (
                        INVOICE_TYPE VARCHAR(20),
                        CONSTRAINT LEGACY_INVOICE_TYPE CHECK (INVOICE_TYPE IN ('BOOKING'))
                    )
                    """);

            new InvoiceDiscriminatorMigration(jdbcTemplate).migrateTable("INVOICES");

            assertDoesNotThrow(() -> jdbcTemplate.update(
                    "INSERT INTO INVOICES (INVOICE_TYPE) VALUES (?)", "BOOKING"));
            assertDoesNotThrow(() -> jdbcTemplate.update(
                    "INSERT INTO INVOICES (INVOICE_TYPE) VALUES (?)", "PAYMENT"));
        } finally {
            dataSource.destroy();
        }
    }
}
