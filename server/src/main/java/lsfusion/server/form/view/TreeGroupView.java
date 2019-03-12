package lsfusion.server.form.view;

import lsfusion.interop.form.layout.AbstractTreeGroup;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.form.entity.FormEntity;
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

public class TreeGroupView extends ComponentView implements ServerIdentitySerializable, PropertyGroupContainerView, AbstractTreeGroup<ComponentView> {
    public static final String TREE_PREFIX = "TREE";
    
    public List<GroupObjectView> groups = new ArrayList<>();

    public TreeGroupEntity entity;

    public ToolbarView toolbarSystem;
    public FilterView userFilter;
    
    public boolean expandOnClick = true;

    @Override
    public String getPropertyGroupContainerSID() {
        return TREE_PREFIX + " " + entity.getSID();
    }

    public TreeGroupView() {
        
    }

    public TreeGroupView(FormView form, TreeGroupEntity entity, Version version) {
        super(entity.getID());

        this.entity = entity;

        for (GroupObjectEntity group : entity.getGroups()) {
            groups.add(form.getNFGroupObject(group, version));
        }

        toolbarSystem = new ToolbarView(form.idGenerator.idShift());
        userFilter = new FilterView(form.idGenerator.idShift());
    }

    @Override
    public double getBaseDefaultFlex(FormEntity formEntity) {
        return 1;
    }

    @Override
    public FlexAlignment getBaseDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.STRETCH;
    }

    @Override
    public ComponentView getToolbarSystem() {
        return toolbarSystem;
    }

    @Override
    public ComponentView getUserFilter() {
        return userFilter;
    }

    public void add(GroupObjectView group) {
        groups.add(group);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, userFilter);

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(expandOnClick);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        
        groups = pool.deserializeList(inStream);
        toolbarSystem = pool.deserializeObject(inStream);
        userFilter = pool.deserializeObject(inStream);

        expandOnClick = inStream.readBoolean();
        
        entity = pool.context.entity.getTreeGroup(ID);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbarSystem.finalizeAroundInit();
        userFilter.finalizeAroundInit();
    }
}
