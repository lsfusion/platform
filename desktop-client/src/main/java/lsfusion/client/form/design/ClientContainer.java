package lsfusion.client.form.design;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.AbstractContainer;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.interop.form.layout.ContainerType.*;

public class ClientContainer extends ClientComponent implements AbstractContainer<ClientComponent, String> {

    private String caption;
    private String description;

    private ContainerType type = ContainerType.CONTAINERH;

    public Alignment childrenAlignment = Alignment.START;

    public int columns = 4;

    public List<ClientComponent> children = new ArrayList<>();

    public ClientContainer() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, description);

        pool.writeObject(outStream, type);

        pool.writeObject(outStream, childrenAlignment);

        outStream.writeInt(columns);
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
        return isSplitHorizontal() || isSplitVertical();
    }

    public boolean isSplitVertical() {
        return type == VERTICAL_SPLIT_PANE;
    }

    public boolean isSplitHorizontal() {
        return type == HORIZONTAL_SPLIT_PANE;
    }

    public boolean isVertical() {
        return isLinearVertical() || isSplitVertical();
    }

    public boolean isHorizontal() {
        return isLinearHorizontal() || isSplitHorizontal();
    }

    public boolean isLinearVertical() {
        return type == CONTAINERV;
    }

    public boolean isLinearHorizontal() {
        return type == CONTAINERH;
    }

    public boolean isLinear() {
        return isLinearVertical() || isLinearHorizontal();
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

    public ClientContainer findContainerByID(int id) {
        if (id == this.ID) return this;
        for (ClientComponent comp : children) {
            if (comp instanceof ClientContainer) {
                ClientContainer result = ((ClientContainer) comp).findContainerByID(id);
                if (result != null) return result;
            }
        }
        return null;
    }

    public ClientContainer findParentContainerBySID(ClientContainer parent, String sID) {
        if (sID.equals(this.sID)) return parent;
        for (ClientComponent comp : children) {
            if (comp instanceof ClientContainer) {
                ClientContainer result = ((ClientContainer) comp).findParentContainerBySID(this, sID);
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
