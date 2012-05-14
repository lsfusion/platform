package platform.server.form.entity;

import platform.base.OrderedMap;
import platform.base.identity.IdentityObject;
import platform.interop.ClassViewType;
import platform.interop.PropertyEditType;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, ServerIdentitySerializable {

    private PropertyEditType editType = PropertyEditType.EDITABLE;

    public PropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    public String mouseBinding;
    public Map<KeyStroke, String> keyBindings;
    public OrderedMap<String, String> contextMenuBindings;
    public Map<String, ActionPropertyObjectEntity> editActions = new HashMap<String, ActionPropertyObjectEntity>();

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public List<GroupObjectEntity> columnGroupObjects = new ArrayList<GroupObjectEntity>();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public CalcPropertyObjectEntity<?> propertyCaption;
    public CalcPropertyObjectEntity<?> propertyReadOnly;
    public CalcPropertyObjectEntity<?> propertyFooter;
    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    public boolean shouldBeLast = false;
    public ClassViewType forceViewType = null;
    public String eventSID = null;

    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P, ?> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        setSID("propertyDraw" + ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<KeyStroke, String>();
        }
        keyBindings.put(ks, actionSID);
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setEditAction(String actionSID, ActionPropertyObjectEntity editAction) {
        editActions.put(actionSID, editAction);
    }

    public void setContextMenuAction(String caption, String actionSID) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<String, String>();
        }
        contextMenuBindings.remove(actionSID);
        contextMenuBindings.put(actionSID, caption);
    }

    public void setKeyEditAction(KeyStroke keyStroke, String actionSID, ActionPropertyObjectEntity editAction) {
        setKeyAction(keyStroke, actionSID);
        setEditAction(actionSID, editAction);
    }

    public void setMouseEditAction(String actionSID, ActionPropertyObjectEntity editAction) {
        setMouseAction(actionSID);
        setEditAction(actionSID, editAction);
    }

    public void setContextMenuEditAction(String caption, String actionSID, ActionPropertyObjectEntity editAction) {
        setContextMenuAction(caption, actionSID);
        setEditAction(actionSID, editAction);
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        columnGroupObjects.add(columnGroupObject);
    }

    public void setPropertyCaption(CalcPropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption;
    }

    public void setPropertyFooter(CalcPropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyBackground(CalcPropertyObjectEntity propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(CalcPropertyObjectEntity propertyForeground) {
        this.propertyForeground = propertyForeground;
    }

    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public boolean isSelector() {
        return editType.equals(PropertyEditType.SELECTOR);
    }

    public boolean isReadOnly() {
        return editType.equals(PropertyEditType.READONLY);
    }

    public boolean isEditable() {
        return editType.equals(PropertyEditType.EDITABLE);
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
        pool.serializeObject(outStream, propertyBackground);
        pool.serializeObject(outStream, propertyForeground);

        outStream.writeBoolean(shouldBeLast);

        outStream.writeBoolean(editType != null);
        if (editType != null)
            outStream.writeByte(editType.serialize());

        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null)
            pool.writeString(outStream, forceViewType.name());

        //todo: serialization/deserialzation
//        pool.writeString(outStream, mouseBinding);
//        outStream.writeInt(keyBinding == null ? 0 : keyBinding.size());
//        if (keyBinding != null) {
//            for (Map.Entry<KeyStroke, String> e : keyBinding.entrySet()) {
//                pool.writeObject(outStream, e.getKey());
//                pool.writeString(outStream, e.getValue());
//            }
//        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        propertyObject = (PropertyObjectEntity<P, ?>) pool.deserializeObject(inStream);
        toDraw = (GroupObjectEntity) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (CalcPropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyReadOnly = (CalcPropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyFooter = (CalcPropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyBackground = (CalcPropertyObjectEntity<?>) pool.deserializeObject(inStream);
        propertyForeground = (CalcPropertyObjectEntity<?>) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean())
            editType = PropertyEditType.deserialize(inStream.readByte());
        if (inStream.readBoolean())
            forceViewType = ClassViewType.valueOf(pool.readString(inStream));
    }

    @Override
    public String toString() {
        return propertyObject.toString();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null?form.getApplyObject(propertyObject.getObjectInstances()):toDraw;        
    }
}
