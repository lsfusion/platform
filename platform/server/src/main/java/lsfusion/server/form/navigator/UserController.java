package lsfusion.server.form.navigator;

import lsfusion.server.logics.DataObject;

import java.sql.SQLException;

public interface UserController {

    void changeCurrentUser(DataObject user) throws SQLException;
    DataObject getCurrentUser();
}
