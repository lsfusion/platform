package lsfusion.server.form.view;

import lsfusion.interop.form.layout.*;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerView extends ComponentView implements AbstractContainer<ContainerView, ComponentView> {

    public List<ComponentView> children = new ArrayList<ComponentView>();

    public String caption;
    public String description;

    private ContainerType type = ContainerType.CONTAINERV;

    public Alignment childrenAlignment = Alignment.LEADING;

    public int gapX = 10;
    public int gapY = 5;
    public int columns = 5;

    public ContainerView() {
    }

    public ContainerView(int ID) {
        super(ID);
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
        //todo: remove
        if (type == ContainerType.CONTAINERH) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        } else if (type == ContainerType.CONTAINERV) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
        } else if (type == ContainerType.COLUMNS) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
        }
    }

    public void setChildrenAlignment(Alignment childrenAlignment) {
        this.childrenAlignment = childrenAlignment;
    }

    public void setGapX(int gapX) {
        this.gapX = gapX;
    }

    public void setGapY(int gapY) {
        this.gapY = gapY;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    @Override
    public ComponentView findById(int id) {
        ComponentView result = super.findById(id);
        if(result!=null) return result;
        
        for(ComponentView child : children) {
            result = child.findById(id);
            if(result!=null) return result;
        }

        return null;
    }

    private void changeContainer(ComponentView comp) {
        if (comp.getContainer() != null)
            comp.getContainer().remove(comp);

        comp.setContainer(this);
    }

    public void add(ComponentView comp) {
        changeContainer(comp);
        children.add(comp);
    }

    public void add(int index, ComponentView comp) {
        changeContainer(comp);
        children.add(index, comp);
    }

    public void addBack(int index, ComponentView comp) {
        add(children.size() - index, comp);
    }

    public void addBefore(ComponentView comp, ComponentView compBefore) {
        if (!children.contains(compBefore)) {
            add(comp);
        } else {
            remove(comp);
            add(children.indexOf(compBefore), comp);
        }
    }

    public void addAfter(ComponentView comp, ComponentView compAfter) {
        if (!children.contains(compAfter)) {
            add(comp);
        } else {
            remove(comp);
            add(children.indexOf(compAfter) + 1, comp);
        }
    }

    public void addFirst(ComponentView comp) {
        add(0, comp);
    }

    public boolean remove(ComponentView comp) {
        if (children.remove(comp)) {
            comp.setContainer(null);
            return true;
        } else {
            return false;
        }
    }

    public boolean isAncestorOf(ComponentView container) {
        return container != null && (super.isAncestorOf(container) || isAncestorOf(container.container));
    }

    public void fillOrderList(List<ContainerView> containers) {
        if(container!=null) container.fillOrderList(containers);
        if(!containers.contains(this)) containers.add(this);
    }

    public List<ComponentView> getChildren() {
        return new ArrayList(children);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children, serializationType);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, description);

        pool.writeObject(outStream, type);

        pool.writeObject(outStream, childrenAlignment);

        outStream.writeInt(columns);
        outStream.writeInt(gapX);
        outStream.writeInt(gapY);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        caption = pool.readString(inStream);
        description = pool.readString(inStream);

        type = pool.readObject(inStream);

        childrenAlignment = pool.readObject(inStream);

        columns = inStream.readInt();
        gapX = inStream.readInt();
        gapY = inStream.readInt();
    }
}
