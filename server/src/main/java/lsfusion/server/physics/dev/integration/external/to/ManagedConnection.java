package lsfusion.server.physics.dev.integration.external.to;

import java.sql.Connection;
import java.sql.SQLException;

final class ManagedConnection implements AutoCloseable {
    private final Connection connection;
    private final boolean shouldClose;

    ManagedConnection(Connection connection, boolean shouldClose) {
        this.connection = connection;
        this.shouldClose = shouldClose;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        if (shouldClose)
            connection.close();
    }
}
