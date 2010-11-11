package platform.server.form.navigator;

import platform.server.logics.DataObject;

public interface UserController {

    void changeCurrentUser(DataObject user);
    DataObject getCurrentUser();
}
