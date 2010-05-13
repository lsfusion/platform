package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.ClientForm;
import platform.client.form.ClientDialog;
import platform.client.form.ClientNavigatorDialog;
import platform.interop.form.RemoteFormInterface;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EventObject;
import java.io.IOException;
import java.rmi.RemoteException;

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
