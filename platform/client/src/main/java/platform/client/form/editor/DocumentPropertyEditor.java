package platform.client.form.editor;

import platform.base.BaseUtils;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
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
        if (KeyStrokes.isSpaceEvent(editEvent)) {
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