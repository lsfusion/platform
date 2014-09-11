package lsfusion.server.form.navigator;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;

import java.sql.SQLException;

public interface UserController {

    boolean changeCurrentUser(DataObject user) throws SQLException, SQLHandledException;
    DataObject getCurrentUser();
}
