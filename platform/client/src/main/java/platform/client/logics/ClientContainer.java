package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.descriptor.CustomConstructible;
import platform.client.descriptor.editor.ComponentEditor;
import platform.base.context.ApplicationContext;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.descriptor.nodes.ContainerNode;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.AbstractContainer;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientContainer extends ClientComponent implements ClientIdentitySerializable, AbstractContainer<ClientContainer, ClientComponent>, CustomConstructible {

    private String title;
    private String description;
    private String sID;

    public List<ClientComponent> children = new ArrayList<ClientComponent>();

    private boolean tabbedPane = false;

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
        pool.writeString(outStream, sID);

        outStream.writeBoolean(tabbedPane);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);
        sID = pool.readString(inStream);

        tabbedPane = inStream.readBoolean();
    }

    public void customConstructor() {
        initAggregateObjects(getContext());
        // по умолчанию, контейнеры не должны resize'ится вообще, то есть не стремится ни к максимальному размеру, ни к предпочитаемому
        // то же самое пока дублируется в ClientContainer
        constraints.fillVertical = -1;
        constraints.fillHorizontal = -1;
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
        return result;
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

    public boolean getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(boolean tabbedPane) {
        this.tabbedPane = tabbedPane;
        updateDependency(this, "tabbedPane");
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

    public List<ClientComponent> getChildren(){
        return children;
    }
}
