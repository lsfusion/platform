package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.client.ClientResourceBundle;
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
        setLatestCurrentDirectory();

        this.extensions = extensions;

        if (description == null || description.isEmpty()) {
            description = ClientResourceBundle.getString("form.editor.allfiles");
        }

        if (BaseUtils.toList(extensions).contains("*.*")) {
            setAcceptAllFileFilterUsed(true);
            if (BaseUtils.toList(extensions).size() == 1) {
                return;
            }
        }
        addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
    }

    public FilePropertyEditor(boolean allFiles) {
        super();
        setLatestCurrentDirectory();

        setAcceptAllFileFilterUsed(allFiles);
    }

    private void setLatestCurrentDirectory() {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        setCurrentDirectory(new File(preferences.get("LATEST_DIRECTORY", "")));
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
            throw new RuntimeException(ClientResourceBundle.getString("form.editor.cant.read.file")+" " + this.getSelectedFile());
        }
    }

    @Override
    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }
}