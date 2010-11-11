package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ActionPropertyEditor implements PropertyEditorComponent {

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return null;
    }

    public boolean valueChanged() {
        return true;
    }
}
