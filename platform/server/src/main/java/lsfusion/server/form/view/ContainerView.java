package lsfusion.server.form.view;

import lsfusion.interop.form.layout.AbstractContainer;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.interop.form.layout.SingleSimplexConstraint;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerView extends ComponentView implements AbstractContainer<ContainerView, ComponentView> {

    public String title;
    public String description;

    private byte type = ContainerType.CONTAINER;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
        if (type == ContainerType.CONTAINERH) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHT;
        } else if (type == ContainerType.CONTAINERV) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
        } else if (type == ContainerType.CONTAINERVH) {
            constraints.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
        }
    }

    public ContainerView() {

    }
    
    public ContainerView(int ID) {
        super(ID);
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getContainerDefaultConstraints(super.getDefaultConstraints());
    }

    public List<ComponentView> children = new ArrayList<ComponentView>();

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

        pool.writeString(outStream, title);
        pool.writeString(outStream, description);

        outStream.writeByte(type);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);

        type = inStream.readByte();
    }
}
