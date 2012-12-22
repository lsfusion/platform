package platform.server.form.view;

import platform.interop.form.layout.AbstractTreeGroup;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.TreeGroupEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupView extends ComponentView implements ServerIdentitySerializable, AbstractTreeGroup<ContainerView, ComponentView> {
    public List<GroupObjectView> groups = new ArrayList<GroupObjectView>();

    public TreeGroupEntity entity;

    public ToolbarView toolbar;
    public FilterView filter;

    public TreeGroupView() {
        
    }

    public TreeGroupView(FormView form, TreeGroupEntity entity) {
        super(entity.getID());

        this.entity = entity;

        for (GroupObjectEntity group : entity.getGroups()) {
            groups.add(form.getGroupObject(group));
        }

        toolbar = new ToolbarView(form.idGenerator.idShift());
        filter = new FilterView(form.idGenerator.idShift());
    }

    @Override
    public ComponentView getToolbar() {
        return toolbar;
    }

    @Override
    public ComponentView getFilter() {
        return filter;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getTreeDefaultConstraints(super.getDefaultConstraints());
    }

    public void add(GroupObjectView group) {
        groups.add(group);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, groups, serializationType);
        pool.serializeObject(outStream, toolbar, serializationType);
        pool.serializeObject(outStream, filter, serializationType);

        outStream.writeBoolean(entity.plainTreeMode);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        
        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        entity = pool.context.entity.getTreeGroup(ID);
    }
}
