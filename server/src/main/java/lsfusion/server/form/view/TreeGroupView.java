package lsfusion.server.form.view;

import lsfusion.interop.form.layout.AbstractTreeGroup;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.TreeGroupEntity;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeGroupView extends ComponentView implements ServerIdentitySerializable, AbstractTreeGroup<ContainerView, ComponentView> {
    public List<GroupObjectView> groups = new ArrayList<>();

    public TreeGroupEntity entity;

    public ToolbarView toolbar;
    public FilterView filter;
    
    public boolean expandOnClick = true;

    public TreeGroupView() {
        
    }

    public TreeGroupView(FormView form, TreeGroupEntity entity, Version version) {
        super(entity.getID());

        this.entity = entity;

        for (GroupObjectEntity group : entity.getGroups()) {
            groups.add(form.getNFGroupObject(group, version));
        }

        toolbar = new ToolbarView(form.idGenerator.idShift());
        filter = new FilterView(form.idGenerator.idShift());

        flex = 1;
        alignment = FlexAlignment.STRETCH;
    }

    @Override
    public ComponentView getToolbar() {
        return toolbar;
    }

    @Override
    public ComponentView getFilter() {
        return filter;
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
        
        outStream.writeBoolean(expandOnClick);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        
        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        expandOnClick = inStream.readBoolean();
        
        entity = pool.context.entity.getTreeGroup(ID);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbar.finalizeAroundInit();
        filter.finalizeAroundInit();
    }
}
