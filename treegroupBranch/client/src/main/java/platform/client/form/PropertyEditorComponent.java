package platform.client.form;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public interface PropertyEditorComponent {

    Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException;

    Object getCellEditorValue() throws RemoteException;
    boolean valueChanged();

}


