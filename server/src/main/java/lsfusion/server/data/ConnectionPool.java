package lsfusion.server.data;

import lsfusion.base.MutableObject;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    Connection getCommon(MutableObject object) throws SQLException; // предполагается что на время и никаких долгих использований
    void returnCommon(MutableObject object, Connection connection) throws SQLException;

    Connection getPrivate(MutableObject object) throws SQLException;
    void returnPrivate(MutableObject object, Connection connection) throws SQLException;
}
