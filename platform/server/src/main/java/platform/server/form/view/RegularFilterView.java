package platform.server.form.view;

import platform.base.OrderedMap;
import platform.base.identity.IdentityObject;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class RegularFilterView extends IdentityObject implements ServerIdentitySerializable {

    public RegularFilterEntity entity;

    public OrderedMap<PropertyDrawView, Boolean> orders = new OrderedMap<PropertyDrawView, Boolean>();

    @SuppressWarnings({"UnusedDeclaration"})
    public RegularFilterView() {

    }
    
    public RegularFilterView(RegularFilterEntity entity) {
        ID = entity.ID;
        this.entity = entity;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, entity.name);
        pool.writeObject(outStream, entity.key);
        outStream.writeBoolean(entity.showKey);

        outStream.writeInt(orders.size());
        for (Map.Entry<PropertyDrawView, Boolean> entry : orders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey(), serializationType);
            outStream.writeBoolean(entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        entity = pool.context.entity.getRegularFilter(ID);

        int orderCount = inStream.readInt();
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawView order = pool.deserializeObject(inStream);
            orders.put(order, inStream.readBoolean());
        }
    }
}
