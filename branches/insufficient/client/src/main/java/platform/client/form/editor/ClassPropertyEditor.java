package platform.client.form.editor;

import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.classes.ClassDialog;
import platform.client.logics.classes.ClientObjectClass;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClassPropertyEditor extends JComponent implements PropertyEditorComponent {

    ClassDialog dialog;

    public ClassPropertyEditor(Component owner, ClientObjectClass baseClass, ClientObjectClass value) {
        dialog = new ClassDialog(owner, baseClass, value, true);
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {

        SwingUtils.requestLocation(dialog, new Point((int)(tableLocation.getX() + cellRectangle.getX()), (int)(tableLocation.getY() + cellRectangle.getMaxY())));
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
