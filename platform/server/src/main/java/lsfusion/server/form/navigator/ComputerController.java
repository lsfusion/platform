package lsfusion.server.form.navigator;

import lsfusion.server.logics.DataObject;

public interface ComputerController {

    DataObject getCurrentComputer();

    boolean isFullClient();
}
