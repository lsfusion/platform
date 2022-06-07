package lsfusion.server.logics.form.interactive.changed;

import com.google.common.base.Throwables;
import lsfusion.base.ApiResourceBundle;
import lsfusion.base.BaseUtils;
import lsfusion.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.StringWithFiles;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;

import static lsfusion.base.BaseUtils.inlineFileSeparator;
import static lsfusion.base.BaseUtils.serializeObject;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    // current (panel) objects
    private final ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects;

    // list (grid) objects
    private final ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects;

    // tree objects
    private final ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects;
    // tree object has + 
    private final ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Integer>> expandables;

    // properties
    private final ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties;
    // property has to be hidden
    private final ImSet<PropertyDrawInstance> dropProperties;

    private final ImList<ComponentView> activateTabs;
    private final ImList<PropertyDrawInstance> activateProps;
    
    private final ImList<ContainerView> collapseContainers;
    private final ImList<ContainerView> expandContainers;

    // current (panel) objects
    private final ImMap<GroupObjectInstance, Boolean> updateStateObjects;

    private final boolean needConfirm;

    public static FormChanges EMPTY = new MFormChanges().immutable();

    public FormChanges(ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects,
                       ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects,
                       ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects,
                       ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Integer>> expandables,
                       ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties,
                       ImSet<PropertyDrawInstance> dropProperties,
                       ImMap<GroupObjectInstance, Boolean> updateStateObjects, ImList<ComponentView> activateTabs, 
                       ImList<PropertyDrawInstance> activateProps, ImList<ContainerView> collapseContainers, 
                       ImList<ContainerView> expandContainers, boolean needConfirm) {
        this.objects = objects;
        this.gridObjects = gridObjects;
        this.parentObjects = parentObjects;
        this.expandables = expandables;
        this.properties = properties;
        this.dropProperties = dropProperties;
        this.updateStateObjects = updateStateObjects;
        this.activateTabs = activateTabs;
        this.activateProps = activateProps;
        this.collapseContainers = collapseContainers;
        this.expandContainers = expandContainers;
        this.needConfirm = needConfirm;
    }

    void out(FormInstance bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.getGroups()) {
            ImList<ImMap<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                System.out.println(group.getID() + " - GRID Changes");
                for (ImMap<ObjectInstance, DataObject> value : groupGridObjects)
                    System.out.println(value);
            }

            ImMap<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null)
                System.out.println(group.getID() + " - Object Changes " + value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for (PropertyReaderInstance property : properties.keyIt()) {
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            System.out.println(property + " ---- property");
            for (ImMap<ObjectInstance, DataObject> gov : propertyValues.keyIt())
                System.out.println(gov + " - " + propertyValues.get(gov));
        }

        System.out.println(" ------- Drop ---------------");
        for (PropertyDrawInstance property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- Activate tab ---------------");
        for (ComponentView tab : activateTabs)
            System.out.println(tab);

        System.out.println(" ------- Activate property ---------------");
        for (PropertyDrawInstance prop : activateProps)
            System.out.println(prop);

        System.out.println(" ------- Collapse containers ---------------");
        for (ContainerView container : collapseContainers)
            System.out.println(container);

        System.out.println(" ------- Expand containers ---------------");
        for (ContainerView container : expandContainers)
            System.out.println(container);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(objects.size());
        for (int i=0,size=objects.size();i<size;i++) {
            outStream.writeInt(objects.getKey(i).getID());
            serializeGroupObjectValue(outStream, objects.getValue(i));
        }

        serializeKeyObjectsMap(outStream, gridObjects);
        serializeKeyObjectsMap(outStream, parentObjects);

        outStream.writeInt(expandables.size());
        for (int i = 0; i < expandables.size(); ++i) {
            outStream.writeInt(expandables.getKey(i).getID());

            ImMap<ImMap<ObjectInstance, DataObject>, Integer> groupExpandables = expandables.getValue(i);
            outStream.writeInt(groupExpandables.size());
            for (int j = 0; j < groupExpandables.size(); ++j) {
                serializeGroupObjectValue(outStream, groupExpandables.getKey(j));
                outStream.writeInt(groupExpandables.getValue(j));
            }
        }

        outStream.writeInt(properties.size());
        for (int i=0,size=properties.size();i<size;i++) {
            PropertyReaderInstance propertyReadInstance = properties.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> rows = properties.getValue(i);

            // сериализация PropertyReadInterface
            outStream.writeByte(propertyReadInstance.getTypeID());
            outStream.writeInt(propertyReadInstance.getID());
            if(propertyReadInstance instanceof PropertyDrawInstance.LastReaderInstance)
                outStream.writeInt(((PropertyDrawInstance.LastReaderInstance) propertyReadInstance).index);

            outStream.writeInt(rows.size());
            for (int j=0,sizeJ=rows.size();j<sizeJ;j++) {
                ImMap<ObjectInstance, DataObject> objectValues = rows.getKey(j);

                serializeGroupObjectValue(outStream, objectValues);

                Object value = rows.getValue(j).getValue();

                if (value instanceof String && ((String) value).contains(inlineFileSeparator)) {
                    String[] parts = ((String) value).split(inlineFileSeparator);

                    int length = parts.length / 2;
                    String[] prefixes = new String[length + 1];
                    String[] names = new String[length];
                    RawFileData[] files = new RawFileData[length];
                    for (int k = 0; k < length + 1; k++) {
                        prefixes[k] = parts[k * 2];
                        if (k * 2 + 1 < parts.length) {
                            String name = parts[k * 2 + 1];
                            Result<String> fullPath = new Result<>();
                            files[k] = ResourceUtils.findResourceAsFileData(name, false, true, fullPath, null);
                            names[k] = fullPath.result;
                        }
                    }

                    value = new StringWithFiles(prefixes, names, files);
                }

                BaseUtils.serializeObject(outStream, value);
            }
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties) {
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeInt(updateStateObjects.size());
        for (int i=0,size=updateStateObjects.size();i<size;i++) {
            outStream.writeInt(updateStateObjects.getKey(i).getID());
            outStream.writeBoolean(updateStateObjects.getValue(i));
        }

        outStream.writeInt(activateTabs.size());
        for (ComponentView activateTab : activateTabs) {
            outStream.writeInt(activateTab.getID());
        }

        outStream.writeInt(activateProps.size());
        for (PropertyDrawInstance propertyView : activateProps) {
            outStream.writeInt(propertyView.getID());
        }
        
        outStream.writeInt(collapseContainers.size());
        for (ContainerView container : collapseContainers) {
            outStream.writeInt(container.getID());
        }
        
        outStream.writeInt(expandContainers.size());
        for (ContainerView container : expandContainers) {
            outStream.writeInt(container.getID());
        }

        outStream.writeBoolean(needConfirm);
    }

    public static void serializeGroupObjectValue(DataOutputStream outStream, ImMap<ObjectInstance,? extends ObjectValue> values) throws IOException {
        outStream.writeInt(values.size());
        for (int i=0,size=values.size();i<size;i++) {
            outStream.writeInt(values.getKey(i).getID());
            serializeObjectValue(outStream, values.getValue(i));
        }
    }

    // should match ClientGroupObjectValue.deserializeObjectValue
    public static void serializeObjectValue(DataOutputStream outStream, ObjectValue value) throws IOException {
        ConcreteClass concreteClass;
        if(value instanceof DataObject && (concreteClass = ((DataObject) value).objectClass) instanceof ConcreteObjectClass) {
            outStream.writeByte(87);
            DataObject objectValue = (DataObject) value;
            outStream.writeLong((Long) objectValue.getValue());
            Long idClass = concreteClass instanceof ConcreteCustomClass ? ((ConcreteCustomClass) concreteClass).ID : null;
            outStream.writeBoolean(idClass != null);
            if(idClass != null)
                outStream.writeLong(idClass);
            return;
        }

        serializeObject(outStream, value.getValue());
    }

    // should match ClientGroupObjectValue.serializeObjectValue
    public static ObjectValue deserializeObjectValue(DataInputStream inStream, ValueClass valueClass) throws IOException {
        byte type = inStream.readByte();
        if(type == 87) {
            long id = inStream.readLong();
            Long idClass = null;
            if(inStream.readBoolean())
                idClass = inStream.readLong();
            return new DataObject(id, ((CustomClass)valueClass).getBaseClass().findConcreteClassID(idClass));
        }

        Serializable value = (Serializable) BaseUtils.deserializeObject(inStream, type);
        if(valueClass instanceof CustomClass) {
            assert value == null;
            return NullValue.instance;
        }

        return ObjectValue.getValue(value, (DataClass)valueClass);
    }

    private void serializeKeyObjectsMap(DataOutputStream outStream, ImMap<GroupObjectInstance, ? extends ImList<ImMap<ObjectInstance, DataObject>>> keyObjects) throws IOException {
        outStream.writeInt(keyObjects.size());
        for (int i=0,size=keyObjects.size();i<size;i++) {

            outStream.writeInt(keyObjects.getKey(i).getID());

            ImList<ImMap<ObjectInstance, DataObject>> rows = keyObjects.getValue(i);
            outStream.writeInt(rows.size());
            for (ImMap<ObjectInstance, DataObject> groupObjectValue : rows) {
                serializeGroupObjectValue(outStream, groupObjectValue);
            }
        }
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    // assert that it was filtered by filterPropertiesExternal
    private void serializePropertiesExternal(JSONObject jsonObject, ImSet<PropertyDrawInstance> serializeProps, ImMap<ObjectInstance, DataObject> gridObjectRow) {
        for(PropertyDrawInstance property : serializeProps)
            jsonObject.put(property.getIntegrationSID(), RemoteForm.formatJSONNull(property.getType(), properties.get(property).get(gridObjectRow).getValue()));
    } 

    private ImSet<PropertyDrawInstance> filterPropertiesExternal(ImSet<PropertyDrawInstance> serializeProps, final boolean panel) {
        return serializeProps.filterFn(property -> !property.isList() == panel && property.isProperty() && property.getIntegrationSID() != null);
    } 

    public JSONObject serializeExternal() {

        // modify
        JSONObject modifyJSON = new JSONObject();
        ImMap<GroupObjectInstance, ImSet<PropertyDrawInstance>> groupProperties = getGroupProperties();
        for (int i=0,size=groupProperties.size();i<size;i++) {
            GroupObjectInstance groupObject = groupProperties.getKey(i);
            if(groupObject == GroupObjectInstance.NULL)
                groupObject = null;
            ImSet<PropertyDrawInstance> properties = groupProperties.getValue(i);

            JSONObject groupObjectJSON = modifyJSON;
            if(groupObject != null) {
                groupObjectJSON = new JSONObject();

                // grid
                boolean updateGridObjects = true;
                ImOrderSet<ImMap<ObjectInstance, DataObject>> rows = gridObjects.get(groupObject);
                if(rows == null) {
                    updateGridObjects = false;
                    rows = groupObject.keys.keyOrderSet();
                }
                ImSet<PropertyDrawInstance> gridProperties = filterPropertiesExternal(properties, false);
                if(rows != null && (updateGridObjects || !gridProperties.isEmpty())) { // has grid and props or keys
                    JSONArray rowsJSON = new JSONArray();
                    for (ImMap<ObjectInstance, DataObject> gridObjectRow : rows) {
                        JSONObject rowJSON = new JSONObject();
                        // grid props
                        serializePropertiesExternal(rowJSON, gridProperties, gridObjectRow);
                        // grid keys (we'll need it anyway, for async deletes)
                        rowJSON.put("value", RemoteForm.formatJSON(groupObject, gridObjectRow));
                        rowsJSON.put(rowJSON);
                    }
                    groupObjectJSON.put("list", rowsJSON);
                }

                // current
                ImMap<ObjectInstance, ? extends ObjectValue> currentObjects = objects.get(groupObject);
                if(currentObjects != null)
                    groupObjectJSON.put("value", RemoteForm.formatJSON(groupObject, currentObjects));

                modifyJSON.put(groupObject.getIntegrationSID(), groupObjectJSON);
            }

            // panel props 
            ImSet<PropertyDrawInstance> panelProperties = filterPropertiesExternal(properties, true);
            serializePropertiesExternal(groupObjectJSON, panelProperties, MapFact.EMPTY());
        }

        // drop props
        JSONArray dropJSON = new JSONArray();
        for (PropertyDrawInstance dropProperty : dropProperties) {
            String integrationSID = dropProperty.getIntegrationSID();
            if (integrationSID != null) {
                dropJSON.put(integrationSID);
            }
        }

        JSONObject response = new JSONObject();
        response.put("modify", modifyJSON);
        response.put("drop", dropJSON);
        return response;
    }

    public ImMap<GroupObjectInstance, ImSet<PropertyDrawInstance>> getGroupProperties() {
        MExclMap<GroupObjectInstance, MExclSet<PropertyDrawInstance>> mGroupProperties = MapFact.mExclMap();
        for(GroupObjectInstance group : gridObjects.keys().merge(objects.keys()))
            mGroupProperties.exclAdd(group, SetFact.mExclSet());
        for (PropertyReaderInstance property : properties.keyIt()) {
            if (property instanceof PropertyDrawInstance) {
                GroupObjectInstance toDraw = ((PropertyDrawInstance) property).toDraw;
                if(toDraw == null)
                    toDraw = GroupObjectInstance.NULL;
                MExclSet<PropertyDrawInstance> mProperties = mGroupProperties.get(toDraw);
                if (mProperties == null) {
                    mProperties = SetFact.mExclSet();
                    mGroupProperties.exclAdd(toDraw, mProperties);
                }
                mProperties.exclAdd((PropertyDrawInstance) property);
            }
        }
        return MapFact.immutable(mGroupProperties);
    }

    public void logChanges(FormInstance bv, Logger logger) {
        logger.trace("getFormChanges:");
        logger.trace("  GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.getGroups()) {
            ImOrderSet<ImMap<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                logger.trace("   " + group.getID() + " - Current grid objects chaned to:");
                for (ImMap<ObjectInstance, DataObject> value : groupGridObjects)
                    logger.trace("     " + value);
            }

            ImMap<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null) {
                logger.trace("   " + group.getID() + " - Current object changed to:  " + value);
            }
        }

        logger.trace("  PROPERTIES ---------------");
        logger.trace("   Values ---------------");
        for (PropertyReaderInstance property : properties.keyIt()) {
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            logger.trace("    " + property + " ---- property");
            for (int i=0,size=propertyValues.size();i<size;i++)
                logger.trace("      " + propertyValues.getKey(i) + " -> " + propertyValues.getValue(i));
        }

        logger.trace("   Droped ---------------");
        for (PropertyDrawInstance property : dropProperties)
            logger.trace("     " + property);

        logger.trace("   Activate tabs ---------------");
        for (ComponentView tab : activateTabs) {
            logger.trace("     " + tab);
        }

        logger.trace("   Activate props ---------------");
        for (PropertyDrawInstance property : activateProps)
            logger.trace("     " + property);

        logger.trace("   Collapse containers ---------------");
        for (ContainerView container : collapseContainers)
            logger.trace("     " + container);

        logger.trace("   Expand containers ---------------");
        for (ContainerView container : expandContainers)
            logger.trace("     " + container);
    }
}
