package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.BaseComponentView;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterGroupView extends BaseComponentView {
    
    public RegularFilterGroupEntity entity;

    public NFOrderSet<RegularFilterView> filters = NFFact.orderSet();

    public RegularFilterGroupView() {
        
    }
    
    public RegularFilterGroupView(RegularFilterGroupEntity entity, Version version) {
        super(entity.ID);

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
    public RegularFilterGroupView(RegularFilterGroupView src, ObjectMapping mapping) {
        super(src, mapping);

        entity = mapping.get(src.entity);
        entity.view = this;

        ID = BaseLogicsModule.generateStaticNewID();

        filters.add(src.filters, mapping::get, mapping.version);
    }
}
