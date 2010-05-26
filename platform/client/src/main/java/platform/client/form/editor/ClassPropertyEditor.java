package platform.client.form.editor;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.classes.ClassDialog;
import platform.client.logics.ClientObjectImplementView;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;
import java.io.IOException;
import java.rmi.RemoteException;

public class ClassPropertyEditor extends JComponent implements PropertyEditorComponent {

    ClassDialog dialog;

    public ClassPropertyEditor(Component owner, ClientObjectClass baseClass, ClientObjectClass value) {
        dialog = new ClassDialog(owner, baseClass, value);
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {

        dialog.setLocation(new Point((int)(tableLocation.getX() + cellRectangle.getX()), (int)(tableLocation.getY() + cellRectangle.getMaxY())));
        dialog.setVisible(true);
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        return dialog.getChosenClass();
    }

    public boolean valueChanged() {
        return dialog.getChosenClass() != null;
    }
}
