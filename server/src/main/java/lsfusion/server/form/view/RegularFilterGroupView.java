package lsfusion.server.form.view;

import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterGroupView extends ComponentView {
    
    public RegularFilterGroupEntity entity;

    public NFOrderSet<RegularFilterView> filters = NFFact.orderSet();

    public RegularFilterGroupView() {
        
    }
    
    public RegularFilterGroupView(RegularFilterGroupEntity entity, Version version) {
        super(entity.ID);
        this.entity = entity;

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
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, filters.getList());
        outStream.writeInt(entity.getDefault());

        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.view.entity)));
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        entity = pool.context.entity.getRegularFilterGroup(ID);
        filters = NFFact.finalOrderSet(pool.<RegularFilterView>deserializeList(inStream));
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        
        filters.finalizeChanges();
    }
}
