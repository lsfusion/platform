package lsfusion.server.data;

import lsfusion.base.MutableObject;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    ExConnection getCommon(MutableObject object) throws SQLException; // предполагается что на время и никаких долгих использований
    void returnCommon(MutableObject object, ExConnection connection) throws SQLException;
    boolean restoreCommon() throws SQLException;

    ExConnection getPrivate(MutableObject object) throws SQLException;
    void returnPrivate(MutableObject object, ExConnection connection) throws SQLException;
}
