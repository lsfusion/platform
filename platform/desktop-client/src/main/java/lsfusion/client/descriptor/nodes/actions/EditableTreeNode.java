package lsfusion.client.descriptor.nodes.actions;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;

public interface EditableTreeNode {
    NodeEditor createEditor(FormDescriptor form);
}
