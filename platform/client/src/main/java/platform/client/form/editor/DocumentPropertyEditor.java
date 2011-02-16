package platform.client.form.editor;

import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EventObject;

public class DocumentPropertyEditor extends FilePropertyEditor {
    Object value;

    public DocumentPropertyEditor(Object value, String description, String... extensions) {
        super(description, extensions);
        this.value = value;
    }

    @Override
    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) throws IOException, ClassNotFoundException {
        if (editEvent instanceof KeyEvent && ((KeyEvent) editEvent).getKeyCode() == KeyEvent.VK_SPACE) {
            returnValue = this.showOpenDialog(null);
        } else {
            openDocument();
        }
        return null;
    }

    public void openDocument() throws IOException {
        if (value != null)
            BaseUtils.openFile((byte[]) value, extensions[0]);
        returnValue = JFileChooser.CANCEL_OPTION;
    }
}