package platform.client.form.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
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
        if (editEvent instanceof KeyEvent) {         // todo [dale]: !!!
            returnValue = this.showOpenDialog(null);
        } else {
            openDocument();
        }
        return null;
    }

    public void openDocument() throws IOException {
        if (value != null) {
            File file = File.createTempFile("lsf", "." + extensions[0]);
            FileOutputStream f = new FileOutputStream(file);
            f.write((byte[]) value);
            f.close();
            Desktop.getDesktop().open(file);
        }
        returnValue = JFileChooser.CANCEL_OPTION;
    }

}