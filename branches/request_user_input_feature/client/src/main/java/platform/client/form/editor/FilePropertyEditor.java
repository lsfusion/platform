package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OSUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.EventObject;

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
        setCurrentDirectory(OSUtils.loadCurrentDirectory());

    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        returnValue = this.showOpenDialog(null);
        return null;
    }

    @Override
    public Object getCellEditorValue() throws RemoteException {
        try {
            OSUtils.saveCurrentDirectory(this.getSelectedFile());
            return returnValue == JFileChooser.APPROVE_OPTION ? IOUtils.getFileBytes(this.getSelectedFile()) : null;
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("form.editor.cant.read.file")+" " + this.getSelectedFile());
        }
    }

    @Override
    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }

    @Override
    public String checkValue(Object value) {
        return null;
    }
}