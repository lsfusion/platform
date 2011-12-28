package platform.server.form.view;

import platform.interop.form.layout.AbstractContainer;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerView extends ComponentView implements AbstractContainer<ContainerView, ComponentView> {

    public String title;
    public String description;

    public boolean tabbedPane = false;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
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

    List<ComponentView> children = new ArrayList<ComponentView>();

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

    public boolean remove(ComponentView comp) {
        if (children.remove(comp)) {
            comp.setContainer(null);
            return true;
        } else {
            return false;
        }
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

        outStream.writeBoolean(tabbedPane);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);

        tabbedPane = inStream.readBoolean();
    }
}
