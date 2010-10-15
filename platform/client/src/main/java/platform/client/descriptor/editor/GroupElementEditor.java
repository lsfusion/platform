package platform.client.descriptor.editor;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;

public class GroupElementEditor extends JPanel implements NodeEditor {

    GroupObjectDescriptor groupObject;

    public GroupElementEditor(GroupObjectDescriptor groupObject) {
        this.groupObject = groupObject;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
