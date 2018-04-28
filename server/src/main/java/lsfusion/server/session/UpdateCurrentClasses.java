package lsfusion.server.session;

import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface UpdateCurrentClasses {

    void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;
}
