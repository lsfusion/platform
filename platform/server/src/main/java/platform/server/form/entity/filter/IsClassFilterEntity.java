package platform.server.form.entity.filter;

import platform.server.classes.CustomClass;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IsClassFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public CustomClass isClass;

    public IsClassFilterEntity() {
        
    }
    
    public IsClassFilterEntity(PropertyObjectEntity<P> iProperty, CustomClass isClass) {
        super(iProperty);
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

        isClass = pool.context.BL.baseClass.findClassID(inStream.readInt());
    }
}
