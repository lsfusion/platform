package lsfusion.server.form.navigator;

import lsfusion.server.logics.DataObject;

public interface FormController {

    void changeCurrentForm(DataObject form);
    DataObject getCurrentForm();
}