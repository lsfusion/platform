package platform.server.form.view;

import platform.base.OrderedMap;
import platform.interop.ToolbarPanelLocation;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegularFilterGroupView extends ComponentView {
    
    public RegularFilterGroupEntity entity;

    private List<RegularFilterView> filters = new ArrayList<RegularFilterView>();

    public OrderedMap<PropertyDrawView, Boolean> nullOrders = new OrderedMap<PropertyDrawView, Boolean>();


    public RegularFilterGroupView() {
        
    }
    
    public RegularFilterGroupView(RegularFilterGroupEntity entity) {
        super(entity.ID);
        setPanelLocation(new ToolbarPanelLocation());
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
        outStream.writeInt(entity.defaultFilter);

        outStream.writeInt(nullOrders.size());
        for (Map.Entry<PropertyDrawView, Boolean> entry : nullOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey(), serializationType);
            outStream.writeBoolean(entry.getValue());
        }

        pool.serializeObject(outStream, pool.context.view.getGroupObject(entity.getToDraw(pool.context.view.entity)));
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        entity = pool.context.entity.getRegularFilterGroup(ID);
        filters = pool.deserializeList(inStream);

        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            nullOrders.put(order, inStream.readBoolean());
        }
    }
}
