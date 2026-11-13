package ug.dankk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class RecyclingDatabase {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path databaseFile = Path.of(
            System.getProperty("user.dir"), "data", "dankk-recycling.db");

    void initialize() throws Exception {
        Files.createDirectories(databaseFile.getParent());
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        supplier TEXT NOT NULL,
                        material TEXT NOT NULL,
                        weight_kg REAL NOT NULL CHECK (weight_kg > 0),
                        rate_cents INTEGER NOT NULL CHECK (rate_cents > 0),
                        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        name TEXT PRIMARY KEY COLLATE NOCASE,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT OR IGNORE INTO suppliers (name, created_at)
                    SELECT DISTINCT supplier, MIN(created_at)
                    FROM transactions
                    GROUP BY supplier
                    """);
        }
    }

    void record(String supplier, String material, BigDecimal weightKg, BigDecimal ratePerKg)
            throws SQLException {
        BigDecimal weight = weightKg.setScale(3, RoundingMode.HALF_UP);
        BigDecimal rate = ratePerKg.setScale(2, RoundingMode.HALF_UP);
        long rateCents = rate.movePointRight(2).longValueExact();
        long amountCents = weight.multiply(rate)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        String sql = """
                INSERT INTO transactions
                    (supplier, material, weight_kg, rate_cents, amount_cents, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (PreparedStatement supplierStatement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO suppliers (name, created_at) VALUES (?, ?)")) {
                supplierStatement.setString(1, supplier);
                supplierStatement.setString(2, LocalDateTime.now().format(TIMESTAMP_FORMAT));
                supplierStatement.executeUpdate();
            }
            statement.setString(1, supplier);
            statement.setString(2, material);
            statement.setBigDecimal(3, weight);
            statement.setLong(4, rateCents);
            statement.setLong(5, amountCents);
            statement.setString(6, LocalDateTime.now().format(TIMESTAMP_FORMAT));
            statement.executeUpdate();
        }
    }

    void addSupplier(String name) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO suppliers (name, created_at) VALUES (?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, LocalDateTime.now().format(TIMESTAMP_FORMAT));
            statement.executeUpdate();
        }
    }

    void renameSupplier(String currentName, String newName) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE suppliers SET name = ? WHERE name = ?")) {
                    statement.setString(1, newName);
                    statement.setString(2, currentName);
                    if (statement.executeUpdate() == 0) {
                            throw new IllegalArgumentException("Supplier was not found.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE transactions SET supplier = ? WHERE supplier = ?")) {
                    statement.setString(1, newName);
                    statement.setString(2, currentName);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    void deleteSupplier(String name) throws SQLException {
        try (Connection connection = connect()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE supplier = ?")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getLong(1) > 0) {
                        throw new IllegalArgumentException(
                                "A supplier with intake records cannot be deleted.");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM suppliers WHERE name = ?")) {
                statement.setString(1, name);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Supplier was not found.");
                }
            }
        }
    }

    List<SupplierSummary> getSuppliers() throws SQLException {
        String sql = """
                SELECT s.name,
                       COUNT(t.id) AS transaction_count,
                       COALESCE(SUM(t.weight_kg), 0) AS total_weight,
                       COALESCE(SUM(t.amount_cents), 0) AS total_value_cents,
                       MAX(t.created_at) AS last_intake_at
                FROM suppliers s
                LEFT JOIN transactions t ON t.supplier = s.name
                GROUP BY s.name
                ORDER BY s.name COLLATE NOCASE
                """;
        List<SupplierSummary> suppliers = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                suppliers.add(new SupplierSummary(
                        result.getString("name"),
                        result.getLong("transaction_count"),
                        result.getDouble("total_weight"),
                        result.getLong("total_value_cents"),
                        result.getString("last_intake_at")));
            }
        }
        return suppliers;
    }

    Summary getSummary() throws SQLException {
        String sql = """
                SELECT COUNT(*) AS transaction_count,
                       COALESCE(SUM(weight_kg), 0) AS total_weight,
                       COALESCE(SUM(amount_cents), 0) AS total_value_cents
                FROM transactions
                """;
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return new Summary(
                    result.getLong("transaction_count"),
                    result.getDouble("total_weight"),
                    result.getLong("total_value_cents"));
        }
    }

    List<Transaction> getRecentTransactions() throws SQLException {
        String sql = """
                SELECT supplier, material, weight_kg, rate_cents, amount_cents, created_at
                FROM transactions
                ORDER BY id DESC
                LIMIT 100
                """;
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                transactions.add(new Transaction(
                        result.getString("created_at"),
                        result.getString("supplier"),
                        result.getString("material"),
                        result.getBigDecimal("weight_kg"),
                        result.getLong("rate_cents"),
                        result.getLong("amount_cents")));
            }
        }
        return transactions;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
    }

    record Summary(long transactionCount, double totalWeightKg, long totalValueCents) {
    }

    record Transaction(
            String createdAt,
            String supplier,
            String material,
            BigDecimal weightKg,
            long rateCents,
            long amountCents) {
    }

    record SupplierSummary(
            String name,
            long transactionCount,
            double totalWeightKg,
            long totalValueCents,
            String lastIntakeAt) {
    }
}
