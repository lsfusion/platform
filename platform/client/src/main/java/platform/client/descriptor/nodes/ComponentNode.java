package platform.client.descriptor.nodes;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClientTreeNode;
import platform.client.logics.ClientComponent;
import platform.interop.serialization.RemoteDescriptorInterface;

public class ComponentNode<T extends ClientComponent, C extends ComponentNode> extends ClientTreeNode<T, C> implements EditableTreeNode {

    public ComponentNode(T component) {
        super(component);
    }

    public ComponentNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public NodeEditor createEditor(FormDescriptor form) {
        return new ComponentEditor("", getTypedObject());
    }
}
