package lsfusion.server.form.view;

import lsfusion.interop.form.layout.SimplexConstraints;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegularFilterGroupView extends ComponentView {
    
    public RegularFilterGroupEntity entity;

    private List<RegularFilterView> filters = new ArrayList<RegularFilterView>();

    public RegularFilterGroupView() {
        
    }
    
    public RegularFilterGroupView(RegularFilterGroupEntity entity) {
        super(entity.ID);
        this.entity = entity;

        for (RegularFilterEntity filterEntity : entity.filters) {
            filters.add(new RegularFilterView(filterEntity));
        }
    }

    public RegularFilterView get(RegularFilterEntity filterEntity) {
        for (RegularFilterView filter : filters) {
            if (filter.entity == filterEntity) {
                return filter;
            }
        }
        return null;
    }

    @Override
    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return SimplexConstraints.getRegularFilterGroupDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, filters);
        outStream.writeInt(entity.defaultFilterIndex);

        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.view.entity)));
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        entity = pool.context.entity.getRegularFilterGroup(ID);
        filters = pool.deserializeList(inStream);
    }
}
