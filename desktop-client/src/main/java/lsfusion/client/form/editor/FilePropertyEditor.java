package lsfusion.client.form.editor;

import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Log;
import lsfusion.client.SwingUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FilePropertyEditor extends DialogBasedPropertyEditor {

    protected final boolean multiple;
    protected final boolean storeName;
    protected final boolean custom;
    protected final String[] extensions;

    protected final JFileChooser fileChooser = new JFileChooser();
    protected boolean canceled;
    private byte[] content;

    public FilePropertyEditor(boolean multiple, boolean storeName, String description, String... extensions) {
        super();

        setLatestCurrentDirectory();

        this.extensions = extensions;
        this.custom = false;
        this.multiple = multiple;
        this.storeName = storeName;

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
    public FilePropertyEditor(boolean multiple, boolean storeName) {
        super();

        setLatestCurrentDirectory();

        this.multiple = multiple;
        this.storeName = storeName;
        this.custom = true;
        this.extensions = null;

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
                content = BaseUtils.filesToBytes(multiple, storeName, custom, files);
            } catch (Exception e) {
                canceled = true;
                Log.error(ClientResourceBundle.getString("form.editor.cant.read.file"), e);
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
        assert !canceled;

        Object res = content;
        content = null;
        return res;
    }
}