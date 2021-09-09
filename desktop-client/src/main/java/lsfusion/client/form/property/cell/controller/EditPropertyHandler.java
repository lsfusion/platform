package lsfusion.client.form.property.cell.controller;

import lsfusion.client.classes.ClientType;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.property.async.ClientInputList;

public interface EditPropertyHandler {
    boolean requestValue(ClientType valueType, Object oldValue, ClientInputList inputList, String actionSID);
    ClientFormController getForm();
    
    void updateEditValue(Object value);
    Object getEditValue();
}
