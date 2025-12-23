package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;

import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterGroupView extends BaseComponentView<RegularFilterGroupView, RegularFilterGroupEntity> {
    
    public final RegularFilterGroupEntity entity;

    public NFOrderSet<RegularFilterView> filters = NFFact.orderSet();

    @Override
    public int getID() {
        return entity.getID();
    }

    @Override
    public String toString() {
        return entity.toString();
    }

    public RegularFilterGroupView(RegularFilterGroupEntity entity, Version version) {
        this.entity = entity;
        this.entity.view = this;

        for (RegularFilterEntity filterEntity : entity.getNFFilters(version)) {
            addFilter(filterEntity, version);
        }
    }
    
    public RegularFilterView addFilter(RegularFilterEntity filterEntity, Version version) {
        RegularFilterView filterView = new RegularFilterView(filterEntity);
        filters.add(filterView, version);
        return filterView; 
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, filters.getList());
        outStream.writeInt(entity.getDefault());

        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.entity)));

        outStream.writeBoolean(entity.noNull);
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        
        filters.finalizeChanges();
    }

    // copy-constructor
    protected RegularFilterGroupView(RegularFilterGroupView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
    }

    @Override
    public void add(RegularFilterGroupView src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.add(filters, src.filters);
    }

    @Override
    public RegularFilterGroupEntity getAddParent(ObjectMapping mapping) {
        return entity;
    }
    @Override
    public RegularFilterGroupView getAddChild(RegularFilterGroupEntity entity, ObjectMapping mapping) {
        return entity.view;
    }
    @Override
    public RegularFilterGroupView copy(ObjectMapping mapping) {
        return new RegularFilterGroupView(this, mapping);
    }
}
