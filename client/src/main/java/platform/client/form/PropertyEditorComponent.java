package platform.client.form;

import java.awt.*;
import java.rmi.RemoteException;

public interface PropertyEditorComponent {

    Component getComponent();

    Object getCellEditorValue() throws RemoteException;
    boolean valueChanged();

}


