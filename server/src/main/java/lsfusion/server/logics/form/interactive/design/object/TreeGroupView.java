package lsfusion.server.logics.form.interactive.design.object;

import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public class TreeGroupView extends GridPropertyView<TreeGroupView, TreeGroupEntity> {
    public static final String TREE_PREFIX = "TREE";

    private NFProperty<Boolean> expandOnClick = NFFact.property();
    private NFProperty<Integer> hierarchicalWidth = NFFact.property();

    private NFProperty<String> hierarchicalCaption = NFFact.property();
    private NFProperty<PropertyObjectEntity> propertyHierarchicalCaption = NFFact.property();

    public List<GroupObjectView> groups = new ArrayList<>();

    public TreeGroupEntity entity;

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

    public TreeGroupView(FormView form, TreeGroupEntity entity, Version version) {
        super(form.genID(), version);

        this.entity = entity;
        this.entity.view = this;

        for (GroupObjectEntity group : entity.getGroups()) {
            groups.add(form.getNFGroupObject(group, version));
        }
    }

    public void add(GroupObjectView group) {
        groups.add(group);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, groups);
        pool.serializeObject(outStream, toolbarSystem);
        pool.serializeObject(outStream, filtersContainer);
        pool.serializeObject(outStream, filterControls);
        pool.serializeCollection(outStream, getFilters());

        outStream.writeBoolean(entity.plainTreeMode);
        
        outStream.writeBoolean(isExpandOnClick());
        outStream.writeInt(getHierarchicalWidth());
        pool.writeString(outStream, getHierarchicalCaption());
    }

    public boolean isExpandOnClick() {
        return nvl(expandOnClick.get(), true);
    }
    public void setExpandOnClick(Boolean value, Version version) {
        expandOnClick.set(value, version);
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

        for(GroupObjectView g : src.groups)
            groups.add(mapping.get(g));

        entity = mapping.get(src.entity);
    }

    @Override
    public void extend(TreeGroupView src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(expandOnClick, src.expandOnClick);
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
