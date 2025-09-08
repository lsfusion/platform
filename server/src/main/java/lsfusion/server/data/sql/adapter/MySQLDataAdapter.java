package lsfusion.server.data.sql.adapter;

import lsfusion.server.data.sql.lambda.SQLRunnable;
import lsfusion.server.data.sql.syntax.MySQLSQLSyntax;
import org.postgresql.replication.LogSequenceNumber;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDataAdapter extends DataAdapter {

    protected static final String DB_NAME = "mysql";

    public MySQLDataAdapter(String database, String server, String user, String password) throws Exception {
        super(MySQLSQLSyntax.instance, database, server, user, password, null);
    }

    public void ensureDB(Server server, boolean cleanDB) throws SQLException {

        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server.host + ":3306/" + dataBase);
        connect.createStatement().execute("DROP DATABASE " + dataBase);
        connect.createStatement().execute("CREATE DATABASE " + dataBase);
        connect.close();
    }

    public Connection createConnection(Server server) throws SQLException {
        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server + ":3306/" + dataBase);
        connect.createStatement().execute("USE " + dataBase);

        return connect;
    }

    @Override
    public LogSequenceNumber getMasterLSN(Connection connection) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getLoad(Server server) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LogSequenceNumber getSlaveLSN(Slave slave) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void ensurePublication(Master master) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void waitForMasterLSN(LogSequenceNumber masterLSN, Slave slave) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void ensureAndEnableSubscription(Slave server, SQLRunnable onFirstStart) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void disableSubscription(Slave slave) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected LogSequenceNumber getMasterLSN() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }
}
