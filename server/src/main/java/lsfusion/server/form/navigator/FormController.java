package lsfusion.server.form.navigator;

import lsfusion.server.logics.ObjectValue;

public interface FormController {

    void changeCurrentForm(ObjectValue form);
    ObjectValue getCurrentForm();
}