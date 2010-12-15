package platform.server.data;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    Connection getCommon(Object object) throws SQLException; // предполагается что на время и никаких долгих использований
    void returnCommon(Object object, Connection connection) throws SQLException;

    Connection getPrivate(Object object) throws SQLException;
    void returnPrivate(Object object, Connection connection) throws SQLException;
}
