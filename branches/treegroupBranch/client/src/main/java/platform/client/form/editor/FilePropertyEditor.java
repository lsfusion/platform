package platform.client.form.editor;

import platform.base.IOUtils;
import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class FilePropertyEditor extends JFileChooser
        implements PropertyEditorComponent {

    protected int returnValue;
    protected String[] extensions;

    public FilePropertyEditor(String description, String... extensions) {
        super();
        this.extensions = extensions;
        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        returnValue = this.showOpenDialog(null);
        return null;
    }

    public Object getCellEditorValue() throws RemoteException {
        try {
            return returnValue == JFileChooser.APPROVE_OPTION ? IOUtils.getFileBytes(this.getSelectedFile()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }
}