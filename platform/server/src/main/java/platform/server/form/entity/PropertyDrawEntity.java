package platform.server.form.entity;

import platform.base.IdentityObject;
import platform.interop.serialization.IdentitySerializable;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.property.PropertyInterface;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, IdentitySerializable {

    public PropertyObjectEntity<P> propertyObject;

    public GroupObjectEntity toDraw;

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public List<GroupObjectEntity> columnGroupObjects = new ArrayList<GroupObjectEntity>();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public PropertyObjectEntity<?> propertyCaption;

    public boolean shouldBeLast = false;
    public Byte forceViewType = null;

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        columnGroupObjects.add(columnGroupObject);
    }

    public void setPropertyCaption(PropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption; 
    }

    public void proceedDefaultDesign(DefaultFormView defaultView) {
        propertyObject.property.proceedDefaultDesign(defaultView, this);
    }

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
//        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
//        pool.serializeObject(propertyCaption);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null) {
            outStream.writeByte(forceViewType);
        }

        //todo:
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:
    }
}
