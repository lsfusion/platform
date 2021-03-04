package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.object.table.tree.AbstractTreeGroup;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;

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

    public int headerHeight = -1;

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

    public void addUserFilter(PropertyDrawView property, Version version) {
        userFilter.addProperty(property, version);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, userFilter);

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(expandOnClick);

        outStream.writeInt(headerHeight);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        
        groups = pool.deserializeList(inStream);
        toolbarSystem = pool.deserializeObject(inStream);
        userFilter = pool.deserializeObject(inStream);

        expandOnClick = inStream.readBoolean();

        headerHeight = inStream.readInt();
        
        entity = pool.context.entity.getTreeGroup(ID);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();

        toolbarSystem.finalizeAroundInit();
        userFilter.finalizeAroundInit();
    }
}
