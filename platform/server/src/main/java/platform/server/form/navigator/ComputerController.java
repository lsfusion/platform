package platform.server.form.navigator;

import platform.server.logics.DataObject;

public interface ComputerController {

    DataObject getCurrentComputer();

    boolean isFullClient();
}
