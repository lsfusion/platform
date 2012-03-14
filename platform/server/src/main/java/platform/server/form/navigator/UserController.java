package platform.server.form.navigator;

import platform.server.logics.DataObject;

import java.sql.SQLException;

public interface UserController {

    void changeCurrentUser(DataObject user) throws SQLException;
    DataObject getCurrentUser();
}
