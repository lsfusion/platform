package platform.client.form.editor;

import platform.base.IOUtils;
import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;
import java.util.prefs.Preferences;

public class FilePropertyEditor extends JFileChooser
        implements PropertyEditorComponent {

    protected int returnValue;
    protected String[] extensions;

    public FilePropertyEditor(String description, String... extensions) {
        super();
        this.extensions = extensions;

        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        setCurrentDirectory(new File(preferences.get("LATEST_DIRECTORY", "")));

        if (description == null || description.isEmpty()) {
            description = "Все файлы";
        }

        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
    }

    public FilePropertyEditor(boolean allFiles) {
        super();
        setAcceptAllFileFilterUsed(allFiles);
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        returnValue = this.showOpenDialog(null);
        return null;
    }

    @Override
    public Object getCellEditorValue() throws RemoteException {
        try {
            Preferences preferences = Preferences.userNodeForPackage(this.getClass());
            preferences.put("LATEST_DIRECTORY", this.getSelectedFile().getAbsolutePath());
            return returnValue == JFileChooser.APPROVE_OPTION ? IOUtils.getFileBytes(this.getSelectedFile()) : null;
        } catch (IOException e) {
            throw new RuntimeException("Не могу прочитать файл: " + this.getSelectedFile());
        }
    }

    @Override
    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }
}