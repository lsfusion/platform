package platform.client.descriptor.nodes.actions;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;

public interface EditableTreeNode {
    NodeEditor createEditor(FormDescriptor form);
}
