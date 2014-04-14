package lsfusion.client.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.ApplicationContext;
import lsfusion.client.descriptor.CustomConstructible;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.descriptor.nodes.ComponentNode;
import lsfusion.client.descriptor.nodes.ContainerNode;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.AbstractContainer;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.interop.form.layout.ContainerType.*;

public class ClientContainer extends ClientComponent implements AbstractContainer<ClientContainer, ClientComponent>, CustomConstructible {

    private String caption;
    private String description;

    private ContainerType type = ContainerType.CONTAINERH;

    public Alignment childrenAlignment = Alignment.LEADING;

    public int columns = 4;

    public int columnLabelsWidth = 0;

    public List<ClientComponent> children = new ArrayList<ClientComponent>();

    public ClientContainer() {
    }

    public ClientContainer(ApplicationContext context) {
        super(context);

        customConstructor();
    }

    public void customConstructor() {
        initAggregateObjects(getContext());
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, description);

        pool.writeObject(outStream, type);

        pool.writeObject(outStream, childrenAlignment);

        outStream.writeInt(columns);
        outStream.writeInt(columnLabelsWidth);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        caption = pool.readString(inStream);
        description = pool.readString(inStream);

        type = pool.readObject(inStream);

        childrenAlignment = pool.readObject(inStream);

        columns = inStream.readInt();
        columnLabelsWidth = inStream.readInt();
    }

    @Override
    public String toString() {
        String result = caption == null ? "" : caption;
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

    public void setCaption(String caption) {
        setRawCaption(caption);
    }

    @Override
    public String getCaption() {
        if (caption == null || caption.equals("")) {
            return (description == null) ? "" : description;
        } else
            return caption;
    }

    //приходится выделять отдельное свойство, чтобы можно было редактировать и при этом возвращать более хитрый caption
    public String getRawCaption() {
        return caption;
    }

    public void setRawCaption(String caption) {
        this.caption = caption;
        updateDependency(this, "rawCaption");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        updateDependency(this, "description");
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
        updateDependency(this, "type");
    }

    public Alignment getChildrenAlignment() {
        return childrenAlignment;
    }

    public void setChildrenAlignment(Alignment childrenAlignment) {
        this.childrenAlignment = childrenAlignment;
        updateDependency(this, "childrenAlignment");
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
        updateDependency(this, "columns");
    }

    public boolean isTabbed() {
        return type == TABBED_PANE;
    }

    public boolean isSplit() {
        return type == HORIZONTAL_SPLIT_PANE || type == VERTICAL_SPLIT_PANE;
    }

    public boolean isVerticalSplit() {
        return type == VERTICAL_SPLIT_PANE;
    }

    public boolean isVertical() {
        return type == CONTAINERV;
    }

    public boolean isHorizontal() {
        return type == CONTAINERH;
    }

    public boolean isLinear() {
        return isVertical() || isHorizontal();
    }

    public boolean isColumns() {
        return type == COLUMNS;
    }

    public boolean isScroll() {
        return type == SCROLL;
    }

    public boolean isFlow() {
        return type == FLOW;
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
}
