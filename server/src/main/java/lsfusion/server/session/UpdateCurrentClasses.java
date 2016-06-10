package lsfusion.server.session;

import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface UpdateCurrentClasses {

    void update(DataSession session) throws SQLException, SQLHandledException;
}
