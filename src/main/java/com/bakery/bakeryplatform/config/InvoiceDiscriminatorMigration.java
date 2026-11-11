package com.bakery.bakeryplatform.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InvoiceDiscriminatorMigration implements ApplicationRunner {
    private static final String INVOICE_TABLE = "INVOICES";
    private static final String PAYMENT_CONSTRAINT = "CHK_INVOICES_PAYMENT_TYPE";
    private static final Pattern INVOICE_TYPE_IN_CHECK =
            Pattern.compile("(?is)^\\s*\\(*\\s*INVOICE_TYPE\\s+IN\\s*\\((.*)\\)\\s*\\)*\\s*$");
    private static final Pattern SINGLE_INVOICE_TYPE_CHECK =
            Pattern.compile("(?is)^\\s*\\(*\\s*INVOICE_TYPE\\s*=\\s*'((?:''|[^'])*)'\\s*\\)*\\s*$");
    private static final Pattern SQL_STRING = Pattern.compile("'((?:''|[^'])*)'");
    private final JdbcTemplate jdbcTemplate;

    public InvoiceDiscriminatorMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateTable(INVOICE_TABLE);
    }

    void migrateTable(String tableName) {
        if (!tableName.matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid invoice table name.");
        }

        List<CheckConstraint> constraints = jdbcTemplate.query("""
                SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
                  ON tc.CONSTRAINT_CATALOG = cc.CONSTRAINT_CATALOG
                 AND tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = CURRENT_SCHEMA()
                  AND tc.TABLE_NAME = ?
                  AND tc.CONSTRAINT_TYPE = 'CHECK'
                """,
                (resultSet, rowNumber) -> new CheckConstraint(
                        resultSet.getString("CONSTRAINT_NAME"),
                        resultSet.getString("CHECK_CLAUSE")),
                tableName);

        for (CheckConstraint constraint : constraints) {
            String clause = constraint.clause().replace("\"", "").trim();
            if (!clause.toUpperCase(Locale.ROOT).contains("INVOICE_TYPE")) {
                continue;
            }

            List<String> values = allowedInvoiceTypes(clause);
            if (values == null) {
                continue;
            }
            if (values.stream().anyMatch("PAYMENT"::equalsIgnoreCase)) {
                continue;
            }
            values.add("PAYMENT");
            replaceConstraint(tableName, constraint.name(), values);
        }
    }

    private static List<String> allowedInvoiceTypes(String clause) {
        Matcher singleValueMatcher = SINGLE_INVOICE_TYPE_CHECK.matcher(clause);
        if (singleValueMatcher.matches()) {
            return new ArrayList<>(List.of(singleValueMatcher.group(1).replace("''", "'")));
        }

        Matcher checkMatcher = INVOICE_TYPE_IN_CHECK.matcher(clause);
        if (!checkMatcher.matches()) {
            return null;
        }
        String expressionValues = checkMatcher.group(1);
        List<String> values = new ArrayList<>();
        Matcher valueMatcher = SQL_STRING.matcher(expressionValues);
        while (valueMatcher.find()) {
            values.add(valueMatcher.group(1).replace("''", "'"));
        }
        String withoutValues = SQL_STRING.matcher(expressionValues).replaceAll("")
                .replace(",", "").trim();
        if (values.isEmpty() || !withoutValues.isEmpty()) {
            return null;
        }
        return values;
    }

    private void replaceConstraint(String tableName, String constraintName, List<String> types) {
        if (!constraintName.matches("[A-Z0-9_]+")) {
            throw new IllegalStateException("Unexpected invoice constraint name.");
        }

        String values = types.stream()
                .map(type -> "'" + type.replace("'", "''") + "'")
                .reduce((first, next) -> first + ", " + next)
                .orElseThrow();
        String table = "\"" + tableName + "\"";
        String oldConstraint = "\"" + constraintName + "\"";
        String newConstraint = "\"" + PAYMENT_CONSTRAINT + "\"";

        jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + oldConstraint);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + newConstraint
                + " CHECK (INVOICE_TYPE IN (" + values + "))");
    }

    private record CheckConstraint(String name, String clause) {
    }
}
