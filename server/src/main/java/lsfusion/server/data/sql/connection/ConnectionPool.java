package lsfusion.server.data.sql.connection;

import lsfusion.base.lambda.EConsumer;
import lsfusion.base.mutability.MutableObject;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.logics.navigator.controller.env.SQLSessionContextProvider;
import lsfusion.server.logics.navigator.controller.env.SQLSessionLSNProvider;
import org.postgresql.replication.LogSequenceNumber;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    LogSequenceNumber getMasterLSN(Connection connection) throws SQLException;
    double getLoad(DataAdapter.Server server) throws SQLException;

    ExConnection getConnection(MutableObject object, DataAdapter.NeedServer needServer, SQLSessionLSNProvider lsn, SQLSessionContextProvider contextProvider) throws SQLException;
    void returnConnection(MutableObject object, ExConnection connection) throws SQLException;

    Connection newRestartConnection(SQLSessionLSNProvider lsn) throws SQLException;
    void closeRestartConnection(Connection connection, EConsumer<Connection, SQLException> cleaner) throws SQLException;

    Connection getBalanceConnection(DataAdapter.NeedServer needServer, SQLSessionLSNProvider lsn) throws SQLException;
    void returnBalanceConnection(Connection prevConnection, EConsumer<Connection, SQLException> cleaner) throws SQLException;

    void registerNeedSavePoint();
    void unregisterNeedSavePoint();
    boolean registerUseSavePoint();
    void unregisterUseSavePoint();
}
