package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;

public class GroupObjectEditor extends JPanel implements NodeEditor {

    public GroupObjectEditor(GroupObjectDescriptor descriptor) {
        add(new JLabel("GroupObject"));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
