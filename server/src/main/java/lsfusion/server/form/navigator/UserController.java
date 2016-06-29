package lsfusion.server.form.navigator;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public interface UserController {

    boolean changeCurrentUser(DataObject user) throws SQLException, SQLHandledException;
    ObjectValue getCurrentUser();
}
