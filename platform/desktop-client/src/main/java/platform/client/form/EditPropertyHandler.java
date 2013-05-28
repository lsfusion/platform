package platform.client.form;

import platform.client.logics.classes.ClientType;

public interface EditPropertyHandler {
    boolean requestValue(ClientType valueType, Object oldValue);
    ClientFormController getForm();
    
    void updateEditValue(Object value);
}
