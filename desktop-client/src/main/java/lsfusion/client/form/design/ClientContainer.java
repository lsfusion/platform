package lsfusion.client.form.design;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.ClientGrid;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.form.design.ContainerType.*;

public class ClientContainer extends ClientComponent {

    public String caption;

    private ContainerType type = ContainerType.CONTAINERH;

    public FlexAlignment childrenAlignment = FlexAlignment.START;

    public int columns = 1;

    public List<ClientComponent> children = new ArrayList<>();

    public ClientContainer() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, caption);

        pool.writeObject(outStream, type);

        pool.writeObject(outStream, childrenAlignment);

        outStream.writeInt(columns);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        caption = pool.readString(inStream);

        type = pool.readObject(inStream);

        childrenAlignment = pool.readObject(inStream);

        columns = inStream.readInt();
    }

    @Override
    public String toString() {
        return caption + " (" + getID() + ")" + "[sid:" + getSID() + "]";
    }

    public FlexAlignment getFlexJustify() {
        return childrenAlignment;
    }

    public void removeFromChildren(ClientComponent component) {
        component.container = null;
        children.remove(component);

        updateDependency(this, "children");
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

    @Override
    public String getCaption() {
        return caption;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
        updateDependency(this, "type");
    }

    public boolean isTabbed() {
        return type == TABBED_PANE;
    }

    public boolean main;

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
        return isLinearVertical() || isSplitVertical() || isColumns();
    }

    public boolean isHorizontal() {
        return isLinearHorizontal() || isSplitHorizontal();
    }


    public boolean isAlignCaptions() {
        if(!isVertical()) // later maybe it makes sense to support align captions for horizontal containers, but with no-wrap it doesn't make much sense
            return false;

        if(children.size() <= 1)
            return false;

        // only simple property draws
        for(ClientComponent child : children) {
            if(!(child instanceof ClientPropertyDraw) || ((ClientPropertyDraw) child).hasColumnGroupObjects() || child.autoSize || child.flex > 0 || ((ClientPropertyDraw) child).panelCaptionVertical)
                return false;
        }

        return true;
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

    public List<ClientComponent> getChildren() {
        return children;
    }

    public final ClientPropertyReader captionReader = new ClientPropertyReader() {
        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            assert BaseUtils.singleKey(readKeys).isEmpty();
            controller.getFormController().setContainerCaption(ClientContainer.this, BaseUtils.nullToString(BaseUtils.singleValue(readKeys)));
        }

        public int getID() {
            return ClientContainer.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CONTAINER_CAPTION;
        }
    };

    public int getFlexCount() {
        if(isTabbed())
            return 0;

        int count = 0;
        for(ClientComponent child : children)
            if(child.getFlex() > 0)
                count++;
        return count;
    }
}
