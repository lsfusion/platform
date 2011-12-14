package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.EventObject;

public class ActionPropertyEditor implements PropertyEditorComponent {

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return true;
    }

    public boolean valueChanged() {
        return true;
    }

    @Override
    public String checkValue(Object value) {
        return null;
    }
}
