package lsfusion.server.data.sql.adapter;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLConsumer;
import lsfusion.server.data.sql.syntax.MySQLSQLSyntax;
import org.postgresql.replication.LogSequenceNumber;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDataAdapter extends DataAdapter {

    protected static final String DB_NAME = "mysql";

    public MySQLDataAdapter(String database, String server, String user, String password) throws Exception {
        super(MySQLSQLSyntax.instance, database, server, user, password, null, false);
    }

    public void ensureDB(String server, boolean cleanDB, boolean master) throws SQLException {

        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server + ":3306/" + dataBase);
        connect.createStatement().execute("DROP DATABASE " + dataBase);
        connect.createStatement().execute("CREATE DATABASE " + dataBase);
        connect.close();
    }

    public Connection createConnection(String server) throws SQLException {
        Connection connect = DriverManager.getConnection("jdbc:mysql://" + server + ":3306/" + dataBase);
        connect.createStatement().execute("USE " + dataBase);

        return connect;
    }

    @Override
    public LogSequenceNumber getMasterLSN(Connection connection) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getLoad(Connection connection) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void runSync(LogSequenceNumber masterLSN, SQLConsumer<Integer> run) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LogSequenceNumber getSlaveLSN(Connection connection) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDBName() {
        return DB_NAME;
    }
}
