package lsfusion.client.descriptor.nodes;

import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.nodes.actions.EditableTreeNode;
import lsfusion.client.logics.ClientComponent;
import lsfusion.client.tree.ClientTreeNode;

public class ComponentNode<T extends ClientComponent, C extends ComponentNode> extends ClientTreeNode<T, C> implements EditableTreeNode {

    public ComponentNode(T component) {
        super(component);
    }

    public ComponentNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new ComponentEditor(getTypedObject());
    }
}
