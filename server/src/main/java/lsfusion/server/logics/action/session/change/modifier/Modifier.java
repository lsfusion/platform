package lsfusion.server.logics.action.session.change.modifier;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.change.PropertyChanges;

import java.sql.SQLException;

public interface Modifier {

    PropertyChanges getPropertyChanges() throws SQLException, SQLHandledException;

}

