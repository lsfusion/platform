package platform.client.form.editor;

import platform.client.logics.classes.ClientObjectClass;

import java.awt.*;
import java.rmi.RemoteException;
import java.text.ParseException;

public class ClassActionPropertyEditor extends ClassPropertyEditor {

    public ClassActionPropertyEditor(Component owner, ClientObjectClass baseClass, ClientObjectClass value) {
        super(owner, baseClass, value);
    }

    public Object getCellEditorValue() throws RemoteException {
        return ((ClientObjectClass)super.getCellEditorValue()).ID; // приходится так извращаться, так как передавать надо не Class, а ID
    }

    @Override
    public String checkValue(Object value){
        return null;
    }
}
