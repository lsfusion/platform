package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.client.logics.classes.ClientObjectClass;

import java.awt.*;
import java.util.EventObject;
import java.io.IOException;
import java.rmi.RemoteException;

public class ClassActionPropertyEditor extends ClassPropertyEditor {

    public ClassActionPropertyEditor(Component owner, ClientObjectClass baseClass, ClientObjectClass value) {
        super(owner, baseClass, value);
    }

    public Object getCellEditorValue() throws RemoteException {
        return ((ClientObjectClass)super.getCellEditorValue()).ID; // приходится так извращаться, так как передавать надо не Class, а ID
    }
}
