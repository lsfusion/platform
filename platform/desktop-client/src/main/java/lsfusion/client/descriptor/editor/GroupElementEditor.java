package lsfusion.client.descriptor.editor;

import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;

import javax.swing.*;

public class GroupElementEditor extends JTabbedPane implements NodeEditor {

    GroupObjectDescriptor groupObject;

    public GroupElementEditor(GroupObjectDescriptor groupObject) {
        this.groupObject = groupObject;
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
