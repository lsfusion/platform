package lsfusion.server.form.navigator;

import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

public interface ConnectionController {

    void changeCurrentConnection(DataObject connection);
    ObjectValue getCurrentConnection();
}