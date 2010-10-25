package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.descriptor.nodes.ContainerNode;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientContainer extends ClientComponent implements ClientIdentitySerializable {

    private String title;
    private String description;
    private String sID;

    public List<ClientComponent> children;

    public ClientContainer() {
        children = new ArrayList<ClientComponent>();
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, title);
        pool.writeString(outStream, description);
        pool.writeString(outStream, sID);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);
        sID = pool.readString(inStream);
    }

    @Override
    public String toString() {
        String result = title == null ? "" : title;
        if (description == null)
            result += " (";
        else
            result += (result.isEmpty() ? "" : " ") + "(" + description + ",";
        result += getID();
        result += ")";
        return result;
    }

    @Override
    public ComponentNode getNode() {
        return new ContainerNode(this);
    }

    public void removeFromChildren(ClientComponent component) {
        children.remove(component);

        IncrementDependency.update(this, "children");
    }

    public void addToChildren(int index, ClientComponent component) {
        if (component.container != null) {
            component.container.removeFromChildren(component);
        }

        children.add(index, component);
        component.container = this;

        IncrementDependency.update(this, "children");
    }

    public void addToChildren(ClientComponent component) {
        addToChildren(children.size(), component);
    }

    public void moveChild(ClientComponent compFrom, ClientComponent compTo) {
        BaseUtils.moveElement(children, compFrom, compTo);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor("Контейнер", this);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        IncrementDependency.update(this, "title");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        IncrementDependency.update(this, "description");
    }

    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public ClientContainer findContainerBySID(String sID) {
        if (sID.equals(this.sID)) return this;
        for (ClientComponent comp : children) {
            if (comp instanceof ClientContainer) {
                ClientContainer result = ((ClientContainer)comp).findContainerBySID(sID);
                if (result != null) return result;
            }
        }
        return null;
    }

    public boolean isAncestorOf(ClientContainer container) {
        return container != null && (equals(container) || isAncestorOf(container.container));
    }
}
