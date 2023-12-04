package lsfusion.client.form.property.table.view;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.async.ClientInputListAction;

import java.util.EventObject;

public interface AsyncChangeInterface {
    EventObject getCurrentEditEvent();
    ClientInputList getCurrentInputList();
    ClientInputListAction[] getCurrentInputListActions();
    String getCurrentActionSID();
    Integer getContextAction();
    void setContextAction(Integer contextAction);

    ClientGroupObjectValue getColumnKey(int row, int col);

    ClientFormController getForm();
    boolean isEditing();
    
    default Object modifyPastedString(String pastedText) { return null; }
}