package platform.client.descriptor.editor;

import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;

public class ObjectEditor extends JPanel implements NodeEditor {

    public ObjectEditor(ObjectDescriptor descriptor) {
        add(new JLabel("Object"));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
