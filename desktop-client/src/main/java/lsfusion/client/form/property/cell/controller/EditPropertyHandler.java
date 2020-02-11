package lsfusion.client.form.property.cell.controller;

import lsfusion.client.classes.ClientType;
import lsfusion.client.form.controller.ClientFormController;

public interface EditPropertyHandler {
    boolean requestValue(ClientType valueType, Object oldValue);
    ClientFormController getForm();
    
    void updateEditValue(Object value);
    Object getEditValue();
}
