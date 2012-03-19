package platform.server.form.entity;

import platform.base.identity.IdentityObject;
import platform.interop.ClassViewType;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, ServerIdentitySerializable {

    public boolean readOnly;
    private boolean selector;

    public PropertyObjectEntity<P> propertyObject;
    
    public GroupObjectEntity toDraw;
    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public List<GroupObjectEntity> columnGroupObjects = new ArrayList<GroupObjectEntity>();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public PropertyObjectEntity<?> propertyCaption;
    public PropertyObjectEntity<?> propertyReadOnly;
    public PropertyObjectEntity<?> propertyFooter;
    public PropertyObjectEntity<?> propertyHighlight;

    public boolean shouldBeLast = false;
    public ClassViewType forceViewType = null;
    public String eventSID = "";

    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        setSID("propertyDraw" + ID);
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

    public void setPropertyFooter(PropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyHighlight(PropertyObjectEntity propertyHighlight) {
        this.propertyHighlight = propertyHighlight;
    }

    public boolean isSelector() {
        return selector;
    }

    public void setSelector(boolean selector) {
        this.selector = selector;
        if (selector)
            readOnly = true;
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView defaultView) {
        propertyObject.property.proceedDefaultDesign(propertyView, defaultView);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
        pool.serializeObject(outStream, propertyCaption);
        pool.serializeObject(outStream, propertyReadOnly);
        pool.serializeObject(outStream, propertyFooter);
        pool.serializeObject(outStream, propertyHighlight);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(readOnly);
        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null) {
            pool.writeString(outStream, forceViewType.name());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        propertyObject = (PropertyObjectEntity<P>) pool.deserializeObject(inStream);
        toDraw = (GroupObjectEntity) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (PropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyReadOnly = (PropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyFooter = (PropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyHighlight = (PropertyObjectEntity<?>) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        readOnly = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));
        }
    }

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null?form.getApplyObject(propertyObject.getObjectInstances()):toDraw;        
    }
}
