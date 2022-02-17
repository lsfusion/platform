package lsfusion.client.form.design;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.property.PropertyReadType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientContainer extends ClientComponent {

    public String caption;
    public boolean collapsible;

    public boolean horizontal;
    public boolean tabbed;

    public FlexAlignment childrenAlignment = FlexAlignment.START;

    public boolean grid;
    public boolean wrap;
    public Boolean alignCaptions;

    public int lines = 1;
    public Integer lineSize = null;
    public Integer captionLineSize = null;
    public boolean lineShrink = false;
    public String customDesign = null;

    public List<ClientComponent> children = new ArrayList<>();
    
    public ClientContainer() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, caption);
        
        outStream.writeBoolean(collapsible);

        pool.writeBoolean(outStream, horizontal);
        pool.writeBoolean(outStream, tabbed);

        pool.writeObject(outStream, childrenAlignment);
        
        outStream.writeBoolean(grid);
        outStream.writeBoolean(wrap);
        outStream.writeBoolean(alignCaptions);

        outStream.writeInt(lines);
        pool.writeInt(outStream, lineSize);
        pool.writeInt(outStream, captionLineSize);
        outStream.writeBoolean(lineShrink);

        outStream.writeBoolean(isCustomDesign());
        if (isCustomDesign())
            pool.writeString(outStream, customDesign);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        caption = pool.readString(inStream);
        
        collapsible = inStream.readBoolean();

        horizontal = pool.readBoolean(inStream);
        tabbed = pool.readBoolean(inStream);

        childrenAlignment = pool.readObject(inStream);
        
        grid = inStream.readBoolean();
        wrap = inStream.readBoolean();
        alignCaptions = pool.readObject(inStream);

        lines = inStream.readInt();
        lineSize = pool.readInt(inStream);
        captionLineSize = pool.readInt(inStream);
        lineShrink = inStream.readBoolean();

        if (inStream.readBoolean())
            customDesign = pool.readString(inStream);
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
        if (component.container != null) {
            component.container.removeFromChildren(component);
        }
        children.add(component);
        component.container = this;
    }

    @Override
    public String getCaption() {
        return getNotNullCaption();
    }
    
    public String getNotNullCaption() {
        return BaseUtils.nullToString(caption);
    }

    public boolean main;

    public boolean isAlignCaptions() {
        if (alignCaptions != null) {
            return alignCaptions;
        }
        
        if(horizontal) // later maybe it makes sense to support align captions for horizontal containers, but with no-wrap it doesn't make much sense
            return false;

        int notActions = 0;
        // only simple property draws
        for(ClientComponent child : children) {
            if(!(child instanceof ClientPropertyDraw) || ((ClientPropertyDraw) child).hasColumnGroupObjects() || (((ClientPropertyDraw)child).autoSize && ((ClientPropertyDraw) child).isAutoDynamicHeight()) || child.flex > 0 || ((ClientPropertyDraw) child).panelCaptionVertical)
                return false;

            if(!((ClientPropertyDraw)child).isAction())
                notActions++;
        }

        return notActions > 1;
    }

    public Integer getLineSize() {
        return lineSize;
    }

    public Integer getCaptionLineSize() {
        return captionLineSize;
    }

    public boolean isCustomDesign() {
        return customDesign != null;
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
            Object containerCaption = BaseUtils.singleValue(readKeys);
            controller.getFormController().setContainerCaption(ClientContainer.this, containerCaption != null ? containerCaption.toString() : null);
        }

        public int getID() {
            return ClientContainer.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CONTAINER_CAPTION;
        }
    };

    public final ClientPropertyReader customPropertyDesignReader = new ClientPropertyReader() {
        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
            //todo ??
        }

        public int getID() {
            return ClientContainer.this.getID();
        }

        public byte getType() {
            return PropertyReadType.CUSTOM;
        }
    };

    public int getFlexCount() {
        if(tabbed)
            return 0;

        int count = 0;
        for(ClientComponent child : children)
            if(child.getFlex() > 0)
                count++;
        return count;
    }
}
