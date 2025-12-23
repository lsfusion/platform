package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerFactory;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.BaseUtils.nvl;

public class TreeGroupView extends GridPropertyView<TreeGroupView, TreeGroupEntity> {
    public static final String TREE_PREFIX = "TREE";

    private NFProperty<Integer> hierarchicalWidth = NFFact.property();

    private NFProperty<String> hierarchicalCaption = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyHierarchicalCaption = NFFact.property();

    public final ImOrderSet<GroupObjectView> groups;

    public TreeGroupEntity entity;

    @Override
    public String toString() {
        return entity.toString();
    }

    @Override
    protected boolean hasPropertyComponent() {
        return super.hasPropertyComponent() || getPropertyHierarchicalCaption() != null;
    }

    @Override
    public String getPropertyGroupContainerSID() {
        return TREE_PREFIX + " " + entity.getSID();
    }

    @Override
    public String getPropertyGroupContainerName() {
        return entity.getSID();
    }

    public TreeGroupView(IDGenerator idGenerator, ContainerFactory<ContainerView> containerFactory, TreeGroupEntity entity, Version version) {
        super(idGenerator, containerFactory, version);

        this.entity = entity;
        this.entity.view = this;

        groups = entity.getGroups().mapOrderSetValues(group -> group.view);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, getFilters());

        outStream.writeBoolean(false);

        outStream.writeInt(getHierarchicalWidth());
        pool.writeString(outStream, getHierarchicalCaption());
    }

    public int getHierarchicalWidth() {
        return nvl(hierarchicalWidth.get(), 0);
    }
    public void setHierarchicalWidth(Integer value, Version version) {
        hierarchicalWidth.set(value, version);
    }

    public String getHierarchicalCaption() {
        return hierarchicalCaption.get();
    }
    public void setHierarchicalCaption(String value, Version version) {
        hierarchicalCaption.set(value, version);
    }

    public PropertyObjectEntity getPropertyHierarchicalCaption() {
        return propertyHierarchicalCaption.get();
    }
    public void setPropertyHierarchicalCaption(PropertyObjectEntity value, Version version) {
        propertyHierarchicalCaption.set(value, version);
    }

    @Override
    protected boolean isCustom() {
        return false;
    }

    // copy-constructor
    protected TreeGroupView(TreeGroupView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);

        groups = mapping.get(src.groups);
    }

    @Override
    public void extend(TreeGroupView src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(hierarchicalWidth, src.hierarchicalWidth);

        mapping.sets(hierarchicalCaption, src.hierarchicalCaption);
        mapping.set(propertyHierarchicalCaption, src.propertyHierarchicalCaption);
    }

    @Override
    public TreeGroupEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public TreeGroupView getAddChild(TreeGroupEntity treeGroupEntity, ObjectMapping mapping) {
        return treeGroupEntity.view;
    }
    @Override
    public TreeGroupView copy(ObjectMapping mapping) {
        return new TreeGroupView(this, mapping);
    }
}
