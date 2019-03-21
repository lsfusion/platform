package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.shared.classes.GType;

public interface GEditPropertyHandler {
    void requestValue(GType valueType, Object oldValue);

    void updateEditValue(Object value);

    void takeFocusAfterEdit();
}
