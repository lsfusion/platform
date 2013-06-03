package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.type.TypeSerializer;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.ChangeReadObjectActionProperty;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class ObjectEntity extends IdentityObject implements PropertyObjectInterfaceEntity, ServerIdentitySerializable {

    public GroupObjectEntity groupTo;

    public String caption;

    public String getCaption() {
        return !BaseUtils.isRedundantString(caption)
               ? caption
               : !BaseUtils.isRedundantString(baseClass.toString())
                 ? baseClass.toString()
                 : ServerResourceBundle.getString("logics.undefined.object");
    }

    public ValueClass baseClass;

    public ObjectEntity() {

    }
    
    public ObjectEntity(int ID, ValueClass baseClass, String caption) {
        this(ID, null, baseClass, caption);
    }

    public ObjectEntity(int ID, String sID, ValueClass baseClass, String caption) {
        super(ID);
        this.sID = sID != null ? sID : "obj" + ID;
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterfaceInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.add(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectEntity) pool.deserializeObject(inStream);
        baseClass = TypeSerializer.deserializeValueClass(pool.context.BL, inStream);
        caption = pool.readString(inStream);
    }

    public PropertyObjectInterfaceEntity getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return this == oldObject
                ? newObject
                : getInstance(instanceFactory).getDataObject();
    }

    @Override
    public String toString() {
        return getCaption();
    }

    @IdentityInstanceLazy
    public ExplicitActionProperty getChangeAction(Property filterProperty) {
        assert baseClass instanceof CustomClass;
        return new ChangeReadObjectActionProperty((CalcProperty) filterProperty, baseClass.getBaseClass());
    }

    @Override
    public AndClassSet getAndClassSet() {
        return baseClass.getUpSet();
    }
}
