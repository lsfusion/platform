package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.OSUtils;
import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FilePropertyEditor extends DialogBasedPropertyEditor {

    protected final boolean multiple;
    protected final boolean custom;
    protected final String[] extensions;

    protected final JFileChooser fileChooser = new JFileChooser();
    protected int returnValue;

    public FilePropertyEditor(boolean multiple, String description, String... extensions) {
        super();

        setLatestCurrentDirectory();

        this.extensions = extensions;
        this.custom = false;
        this.multiple = multiple;

        if (description == null || description.isEmpty()) {
            description = ClientResourceBundle.getString("form.editor.allfiles");
        }

        if (BaseUtils.toList(extensions).contains("*.*")) {
            fileChooser.setAcceptAllFileFilterUsed(true);
            if (BaseUtils.toList(extensions).size() == 1) {
                return;
            }
        }
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
        fileChooser.setMultiSelectionEnabled(multiple);
    }

    //custom constructor
    public FilePropertyEditor(boolean multiple) {
        super();

        setLatestCurrentDirectory();

        this.multiple = multiple;
        this.custom = true;
        this.extensions = null;

        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setMultiSelectionEnabled(multiple);
    }

    private void setLatestCurrentDirectory() {
        fileChooser.setCurrentDirectory(OSUtils.loadCurrentDirectory());
    }

    @Override
    public void showDialog(Point desiredLocation) {
        returnValue = fileChooser.showOpenDialog(SwingUtils.getActiveWindow());
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            OSUtils.saveCurrentDirectory(fileChooser.getSelectedFile());
        }
    }

    @Override
    public boolean valueChanged() {
        return returnValue == JFileChooser.APPROVE_OPTION;
    }

    @Override
    public Object getCellEditorValue() {
        assert returnValue == JFileChooser.APPROVE_OPTION;

        File[] files = multiple ? fileChooser.getSelectedFiles() : new File[]{fileChooser.getSelectedFile()};

        try {
            return BaseUtils.filesToBytes(multiple, custom, files);
        } catch (IOException e) {
            throw new RuntimeException(ClientResourceBundle.getString("form.editor.cant.read.file") + " " + fileChooser.getSelectedFile());
        }
    }
}