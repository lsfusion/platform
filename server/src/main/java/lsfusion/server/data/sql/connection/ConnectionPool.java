package lsfusion.server.data.sql.connection;

import lsfusion.base.mutability.MutableObject;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    ExConnection getCommon(MutableObject object) throws SQLException; // предполагается что на время и никаких долгих использований
    void returnCommon(MutableObject object, ExConnection connection) throws SQLException;
    boolean restoreCommon(Connection connection) throws SQLException;

    ExConnection getPrivate(MutableObject object) throws SQLException;
    void returnPrivate(MutableObject object, ExConnection connection) throws SQLException;

    Connection newRestartConnection() throws SQLException;
    void closeRestartConnection(Connection connection) throws SQLException;

    void registerNeedSavePoint();
    void unregisterNeedSavePoint();
    boolean registerUseSavePoint();
    void unregisterUseSavePoint();
}
