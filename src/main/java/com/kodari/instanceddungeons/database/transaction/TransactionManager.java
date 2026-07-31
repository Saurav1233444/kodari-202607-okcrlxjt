package com.kodari.instanceddungeons.database.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class TransactionManager {
    private final DataSource dataSource;
    private final ThreadLocal<TransactionContext> current = new ThreadLocal<>();

    public TransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public <T> T inTransaction(TransactionCallback<T> callback) throws SQLException {
        Objects.requireNonNull(callback, "callback");
        TransactionContext existing = current.get();
        if (existing != null) {
            try {
                return callback.execute(existing.connection);
            } catch (SQLException | RuntimeException exception) {
                existing.rollbackOnly = true;
                throw exception;
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            TransactionContext context = new TransactionContext(connection);
            current.set(context);
            try {
                T result = callback.execute(connection);
                if (context.rollbackOnly) {
                    connection.rollback();
                    throw new SQLException("Nested transaction marked the transaction for rollback.");
                }
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                current.remove();
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private static final class TransactionContext {
        private final Connection connection;
        private boolean rollbackOnly;

        private TransactionContext(Connection connection) {
            this.connection = connection;
        }
    }
}