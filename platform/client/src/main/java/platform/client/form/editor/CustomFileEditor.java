package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;
import java.util.prefs.Preferences;

public class CustomFileEditor extends DocumentPropertyEditor {
    boolean allowOpen;
    boolean multiple;
    boolean custom;

    //custom constructor
    public CustomFileEditor(Object value, boolean allowOpen, boolean multiple) {
        super(value, true);
        this.allowOpen = allowOpen;
        this.multiple = multiple;
        this.custom = true;
        setMultiSelectionEnabled(multiple);
    }

    //fixed file format constructor
    public CustomFileEditor(Object value, boolean allowOpen, boolean multiple, String description, String[] extensions) {
        super(value, description, extensions);
        this.allowOpen = allowOpen;
        this.multiple = multiple;
        this.custom = false;
        setMultiSelectionEnabled(multiple);
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        if (allowOpen) {
            return super.getComponent(tableLocation, cellRectangle, editEvent);
        } else {
            returnValue = this.showOpenDialog(SwingUtils.getActiveWindow());
        }
        return null;
    }


    //переопределяются те методы, в которых происходят манипуляции с массивом байт
    @Override
    public Object getCellEditorValue() throws RemoteException {
        File[] files = isMultiSelectionEnabled() ? getSelectedFiles() : new File[]{getSelectedFile()};

        Preferences preferences = Preferences.userNodeForPackage(Main.class);
        preferences.put("LATEST_DIRECTORY", files[0].getAbsolutePath());

        byte result[] = null;

        try {
            result = ((!multiple) && (!custom)) ? IOUtils.getFileBytes(files[0]) : BaseUtils.filesToBytes(multiple, custom, files);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return returnValue == JFileChooser.APPROVE_OPTION ? result : null;
    }

    @Override
    public void openDocument() throws IOException {
        if (value != null) {
            byte[] union = (byte[]) value;
            if ((!multiple) && (!custom))
                BaseUtils.openFile(union, extensions[0]);
            else
                BaseUtils.openFile(BaseUtils.getFile(union), BaseUtils.getExtension(union));
        }
        returnValue = JFileChooser.CANCEL_OPTION;
    }
}
