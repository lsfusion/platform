package platform.client.form.editor;

import platform.base.IOUtils;
import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;

public class FileActionPropertyEditor extends JFileChooser
        implements PropertyEditorComponent {

    private int returnValue;

    public FileActionPropertyEditor(String filterDescription, String... fileExtensions) {
        super();

        if (filterDescription == null || filterDescription.isEmpty()) {
            filterDescription = "Все файлы";
        }

        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(new FileNameExtensionFilter(filterDescription, fileExtensions));
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        returnValue = this.showOpenDialog(null);
        return null;
    }

    public Object getCellEditorValue() {
        try {
            return returnValue == JFileChooser.APPROVE_OPTION ? IOUtils.getFileBytes(this.getSelectedFile()) : null;
        } catch (IOException e) {
            throw new RuntimeException("Не могу прочитать файл: " + this.getSelectedFile());
        }
    }

    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }
}