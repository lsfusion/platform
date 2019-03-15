package lsfusion.server.logics.action.session.classes.change;

import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface UpdateCurrentClasses {

    void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;
}
