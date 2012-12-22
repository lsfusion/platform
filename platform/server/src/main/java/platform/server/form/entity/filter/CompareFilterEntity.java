package platform.server.form.entity.filter;

import platform.interop.Compare;
import platform.server.classes.DataClass;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.OrderEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class CompareFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public Compare compare;

    public OrderEntity<?> value;

    // нельзя удалять - используется при сериализации
    public CompareFilterEntity() {
    }

    public CompareFilterEntity(CalcPropertyObjectEntity<P> property, Compare compare, OrderEntity<?> value) {
        this(property, compare, value, true);
    }

    public CompareFilterEntity(CalcPropertyObjectEntity<P> property, Compare compare, Object value) {
        this(property, compare, new DataObject(value, (DataClass) property.property.getType()));
    }

    public CompareFilterEntity(CalcPropertyObjectEntity<P> property, Compare compare, OrderEntity<?> value, boolean resolveAdd) {
        super(property, resolveAdd);
        this.value = value;
        this.compare = compare;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
         return instanceFactory.getInstance(this);
    }

    @Override
    protected void fillObjects(Set<ObjectEntity> objects) {
        super.fillObjects(objects);
        value.fillObjects(objects);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new CompareFilterEntity<P>(property.getRemappedEntity(oldObject, newObject, instanceFactory), compare, value.getRemappedEntity(oldObject, newObject, instanceFactory), resolveAdd);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        compare.serialize(outStream);
        pool.serializeObject(outStream, value);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        compare = Compare.deserialize(inStream);
        value = pool.deserializeObject(inStream);
    }
}
