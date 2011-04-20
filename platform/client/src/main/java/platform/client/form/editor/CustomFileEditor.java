package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.EventObject;
import java.util.prefs.Preferences;

public class CustomFileEditor extends DocumentPropertyEditor {
    boolean allowOpen;


    public CustomFileEditor(Object value, boolean allowOpen) {
        super(value, true);
        this.allowOpen = allowOpen;
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        if (allowOpen) {
            return super.getComponent(tableLocation, cellRectangle, editEvent);
        } else {
            returnValue = this.showOpenDialog(null);
        }
        return null;
    }


    //переопределяются те методы, в которых происходят манипуляции с массивом байт
    @Override
    public Object getCellEditorValue() throws RemoteException {
        try {
            Preferences preferences = Preferences.userNodeForPackage(this.getClass());
            preferences.put("LATEST_DIRECTORY", this.getSelectedFile().getAbsolutePath());
            File file = this.getSelectedFile();
            String name = file.getName();
            int index = name.lastIndexOf(".");
            String extension = name.substring(index + 1);
            byte[] ext = extension.getBytes();
            byte[] f = IOUtils.getFileBytes(file);
            byte[] union = new byte[ext.length + f.length + 1];
            union[0] = (byte) ext.length;
            System.arraycopy(ext, 0, union, 1, ext.length);
            System.arraycopy(f, 0, union, 1 + ext.length, f.length);

            return returnValue == JFileChooser.APPROVE_OPTION ? union : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void openDocument() throws IOException {
        if (value != null) {
            byte[] union = (byte[]) value;
            byte ext[] = new byte[union[0]];
            byte file[] = new byte[union.length - union[0] - 1];
            System.arraycopy(union, 1, ext, 0, ext.length);
            System.arraycopy(union, 1 + ext.length, file, 0, file.length);

            BaseUtils.openFile(file, new String(ext));

        }
        returnValue = JFileChooser.CANCEL_OPTION;
    }
}
