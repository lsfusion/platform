package lsfusion.server.form.entity.filter;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IsClassFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public CustomClass isClass;

    public IsClassFilterEntity(CalcPropertyObjectEntity<P> property, CustomClass isClass, boolean resolveAdd) {
        super(property, resolveAdd);
        this.isClass = isClass;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        
        isClass.serialize(outStream);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        isClass = pool.context.BL.LM.baseClass.findClassID(inStream.readInt());
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new IsClassFilterEntity<P>(property.getRemappedEntity(oldObject, newObject, instanceFactory), isClass, resolveAdd);
    }


}
