package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

public interface ExecuteEditContext extends EditContext {

    boolean isReadOnly();

    void trySetFocus();
}
