package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.context.ApplicationContext;
import platform.client.descriptor.CustomConstructible;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.descriptor.nodes.ContainerNode;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.AbstractContainer;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static platform.interop.form.layout.ContainerType.SPLIT_PANE_VERTICAL;

public class ClientContainer extends ClientComponent implements AbstractContainer<ClientContainer, ClientComponent>, CustomConstructible {

    private String title;
    private String description;

    public List<ClientComponent> children = new ArrayList<ClientComponent>();

    private byte type = ContainerType.CONTAINER;

    public ClientContainer() {
    }

    public ClientContainer(ApplicationContext context) {
        super(context);

        customConstructor();
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, title);
        pool.writeString(outStream, description);

        outStream.writeByte(type);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);

        type = inStream.readByte();
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getContainerDefaultConstraints(super.getDefaultConstraints());
    }

    public void customConstructor() {
        initAggregateObjects(getContext());
    }

    @Override
    public String getCaption() {
        if (title == null || title.equals("")) {
            return (description == null) ? "" : description;
        } else
            return title;
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
        return result + "[sid:" + getSID() + "]";
    }

    @Override
    public ComponentNode getNode() {
        return new ContainerNode(this);
    }

    public void removeFromChildren(ClientComponent component) {
        component.container = null;
        children.remove(component);

        updateDependency(this, "children");
    }

    public void addToChildren(int index, ClientComponent component) {
        add(index, component);
        updateDependency(this, "children");
    }

    public void addToChildren(ClientComponent component) {
        addToChildren(children.size(), component);
    }

    public void add(ClientComponent component) {
        add(children.size(), component);
    }

    public void add(int index, ClientComponent component) {
        if (component.container != null) {
            component.container.removeFromChildren(component);
        }
        children.add(index, component);
        component.container = this;
    }

    public void moveChild(ClientComponent compFrom, ClientComponent compTo) {
        BaseUtils.moveElement(children, compFrom, compTo);
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        updateDependency(this, "title");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateDependency(this, "description");
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getStringType() {  // usage через reflection
        return ContainerType.getTypeNamesList().get((int) type);
    }

    public void setStringType(String type) {
        this.type = (byte) ContainerType.getTypeNamesList().indexOf(type);
        updateDependency(this, "stringType");
    }

    public boolean isTabbedPane() {
        return type == ContainerType.TABBED_PANE;
    }

    public boolean isSplitPane() {
        return type == ContainerType.SPLIT_PANE_HORIZONTAL || type == SPLIT_PANE_VERTICAL;
    }

    @Override
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
                ClientContainer result = ((ClientContainer) comp).findContainerBySID(sID);
                if (result != null) return result;
            }
        }
        return null;
    }

    public boolean isAncestorOf(ClientContainer container) {
        return container != null && (equals(container) || isAncestorOf(container.container));
    }

    public List<ClientComponent> getChildren() {
        return children;
    }

    public String getCodeClass() {
        return "ContainerView";
    }

    @Override
    public String getCodeConstructor() {
        return "design.createContainer(" +
                (title == null ? null : ("\"" + title + "\""))
                + ", \"" + description + "\", " + (sID == null ? sID : ("\"" + sID + "\"")) + ")";
    }
}
