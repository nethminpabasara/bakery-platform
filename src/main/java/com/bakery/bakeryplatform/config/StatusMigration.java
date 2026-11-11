package com.bakery.bakeryplatform.config;

import com.bakery.bakeryplatform.model.order.OrderStatus;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Extend H2 status columns while keeping values from older databases. */
@Component
public class StatusMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public StatusMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrate();
    }

    void migrate() {
        extendStatus("PAYMENTS", PaymentStatus.PENDING);
        extendStatus("ORDERS", OrderStatus.values());
    }

    private void extendStatus(String table, Enum<?>... supportedValues) {
        List<String> identifiers = jdbc.queryForList("""
                SELECT DTD_IDENTIFIER FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = ?
                  AND COLUMN_NAME = 'STATUS' AND DATA_TYPE = 'ENUM'
                """, String.class, table);
        if (identifiers.isEmpty()) {
            return;
        }

        List<String> existing = jdbc.queryForList("""
                SELECT VALUE_NAME FROM INFORMATION_SCHEMA.ENUM_VALUES
                WHERE OBJECT_SCHEMA = CURRENT_SCHEMA() AND OBJECT_NAME = ?
                  AND ENUM_IDENTIFIER = ? ORDER BY VALUE_ORDINAL
                """, String.class, table, identifiers.getFirst());

        Set<String> statuses = new LinkedHashSet<>(existing);
        for (Enum<?> value : supportedValues) {
            statuses.add(value.name());
        }
        if (statuses.size() > existing.size()) {
            String values = statuses.stream().map(value -> "'" + value.replace("'", "''") + "'")
                    .collect(Collectors.joining(","));
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN STATUS ENUM(" + values + ")");
        }
    }
}
