package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FilePropertyEditor extends DialogBasedPropertyEditor {

    protected final boolean multiple;
    protected final boolean storeName;
    protected final boolean custom;
    protected final String[] extensions;
    protected final boolean named;

    protected final JFileChooser fileChooser = new JFileChooser();
    protected boolean canceled;
    private Object content; // RawFileData, FileData or byte[] (if multiple || storeName)

    public FilePropertyEditor(boolean multiple, boolean storeName, String description, String... extensions) {
        super();

        setLatestCurrentDirectory();

        this.extensions = extensions;
        this.custom = false;
        this.multiple = multiple;
        this.storeName = storeName;
        this.named = false;

        if (description == null || description.isEmpty()) {
            description = ClientResourceBundle.getString("form.editor.allfiles");
        }

        if (BaseUtils.toList(extensions).contains("*.*")) {
            fileChooser.setAcceptAllFileFilterUsed(true);
            if (BaseUtils.toList(extensions).size() == 1) {
                return;
            }
        }
        if(fileChooser.isAcceptAllFileFilterUsed()) {
            fileChooser.setAcceptAllFileFilterUsed(false);
            if(!emptyValidExtension()) {
                fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
            }
            fileChooser.setAcceptAllFileFilterUsed(true);
        } else {
            fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(description, extensions));
        }
        fileChooser.setMultiSelectionEnabled(multiple);
    }

    private boolean emptyValidExtension() {
        return extensions.length == 1 && extensions[0].isEmpty();
    }

    //custom constructor
    public FilePropertyEditor(boolean multiple, boolean storeName, boolean named) {
        super();

        setLatestCurrentDirectory();

        this.multiple = multiple;
        this.storeName = storeName;
        this.custom = true;
        this.extensions = null;
        this.named = named;

        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setMultiSelectionEnabled(multiple);
    }

    private void setLatestCurrentDirectory() {
        fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
    }

    @Override
    public void showDialog(Point desiredLocation) {
        canceled = fileChooser.showOpenDialog(SwingUtils.getActiveWindow()) != JFileChooser.APPROVE_OPTION;
        if (!canceled) {
            File[] files = multiple ? fileChooser.getSelectedFiles() : new File[]{fileChooser.getSelectedFile()};

            try {
                content = BaseUtils.filesToBytes(multiple, storeName, custom, named, null, files);
            } catch (Exception e) {
                canceled = true;
                return;
            }

            SystemUtils.saveCurrentDirectory(fileChooser.getSelectedFile());
        }
    }

    @Override
    public boolean valueChanged() {
        return !canceled;
    }

    @Override
    public Object getCellEditorValue() {
        // закомментил, т.к. не выполнялся при показе диалога в отборе  
//        assert !canceled;

        Object res = content;
        content = null;
        return res;
    }
}