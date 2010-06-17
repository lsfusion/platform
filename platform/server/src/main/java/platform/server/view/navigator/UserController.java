package platform.server.view.navigator;

import platform.server.logics.DataObject;

public interface UserController {

    void changeCurrentUser(DataObject user);
    DataObject getCurrentUser();
}
