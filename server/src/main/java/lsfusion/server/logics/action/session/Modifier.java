package lsfusion.server.logics.action.session;

import lsfusion.server.data.SQLHandledException;

import java.sql.SQLException;

public interface Modifier {

    PropertyChanges getPropertyChanges() throws SQLException, SQLHandledException;

}

