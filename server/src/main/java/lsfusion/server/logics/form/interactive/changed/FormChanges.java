package lsfusion.server.logics.form.interactive.changed;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.serializeObject;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    private final ImMap<GroupObjectInstance, ClassViewType> classViews;

    // current (panel) objects
    private final ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects;

    // value.keySet() из key.getUpTreeGroups
    private final ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects;

    // tree objects
    private final ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects;
    // tree object has + 
    private final ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Boolean>> expandables;

    private final ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties;

    private final ImSet<PropertyDrawInstance> panelProperties;
    private final ImSet<PropertyDrawInstance> dropProperties;

    private final ImList<ComponentView> activateTabs;
    private final ImList<PropertyDrawInstance> activateProps;

    public static FormChanges EMPTY = new MFormChanges().immutable();

    public FormChanges(ImMap<GroupObjectInstance, ClassViewType> classViews, ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects,
                       ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects,
                       ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects,
                       ImMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Boolean>> expandables,
                       ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties,
                       ImSet<PropertyDrawInstance> panelProperties, ImSet<PropertyDrawInstance> dropProperties, 
                       ImList<ComponentView> activateTabs, ImList<PropertyDrawInstance> activateProps) {
        this.classViews = classViews;
        this.objects = objects;
        this.gridObjects = gridObjects;
        this.parentObjects = parentObjects;
        this.expandables = expandables;
        this.properties = properties;
        this.panelProperties = panelProperties;
        this.dropProperties = dropProperties;
        this.activateTabs = activateTabs;
        this.activateProps = activateProps;
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

        System.out.println(" ------- PANEL ---------------");
        for (PropertyDrawInstance property : panelProperties)
            System.out.println(property);

        System.out.println(" ------- Drop ---------------");
        for (PropertyDrawInstance property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- Activate tab ---------------");
        for (ComponentView tab : activateTabs)
            System.out.println(tab);

        System.out.println(" ------- Activate property ---------------");
        for (PropertyDrawInstance prop : activateProps)
            System.out.println(prop);

        System.out.println(" ------- CLASSVIEWS ---------------");
        for (int i=0,size=classViews.size();i<size;i++) {
            System.out.println(classViews.getKey(i) + " - " + classViews.getValue(i));
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (int i=0,size=classViews.size();i<size;i++) {
            outStream.writeInt(classViews.getKey(i).getID());
            outStream.writeInt(classViews.getValue(i).ordinal());
        }

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

            ImMap<ImMap<ObjectInstance, DataObject>, Boolean> groupExpandables = expandables.getValue(i);
            outStream.writeInt(groupExpandables.size());
            for (int j = 0; j < groupExpandables.size(); ++j) {
                serializeGroupObjectValue(outStream, groupExpandables.getKey(j));
                outStream.writeBoolean(groupExpandables.getValue(j));
            }
        }

        outStream.writeInt(properties.size());
        for (int i=0,size=properties.size();i<size;i++) {
            PropertyReaderInstance propertyReadInstance = properties.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> rows = properties.getValue(i);

            // сериализация PropertyReadInterface
            outStream.writeByte(propertyReadInstance.getTypeID());
            outStream.writeInt(propertyReadInstance.getID());

            outStream.writeInt(rows.size());
            for (int j=0,sizeJ=rows.size();j<sizeJ;j++) {
                ImMap<ObjectInstance, DataObject> objectValues = rows.getKey(j);

                serializeGroupObjectValue(outStream, objectValues);

                serializeObject(outStream, rows.getValue(j).getValue());
            }
        }

        outStream.writeInt(panelProperties.size());
        for (PropertyDrawInstance propertyDraw : panelProperties) {
            outStream.writeInt(propertyDraw.getID());
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties) {
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeInt(activateTabs.size());
        for (ComponentView activateTab : activateTabs) {
            outStream.writeInt(activateTab.getID());
        }

        outStream.writeInt(activateProps.size());
        for (PropertyDrawInstance propertyView : activateProps) {
            outStream.writeInt(propertyView.getID());
        }
    }

    private void serializeGroupObjectValue(DataOutputStream outStream, ImMap<ObjectInstance,? extends ObjectValue> values) throws IOException {
        outStream.writeInt(values.size());
        for (int i=0,size=values.size();i<size;i++) {
            outStream.writeInt(values.getKey(i).getID());
            serializeObject(outStream, values.getValue(i).getValue());
        }
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

    public String serializeExternal() {
        JSONObject modifyJSON = new JSONObject();

        Map<GroupObjectInstance, List<PropertyDrawInstance>> groupObjectPropertiesMap = new HashMap<>();
        for (PropertyReaderInstance property : properties.keyIt()) {
            if (property instanceof PropertyDrawInstance) {
                GroupObjectInstance toDraw = ((PropertyDrawInstance) property).toDraw;
                List<PropertyDrawInstance> propertiesList = groupObjectPropertiesMap.get(toDraw);
                if (propertiesList == null) {
                    propertiesList = new ArrayList<>();
                }
                propertiesList.add((PropertyDrawInstance) property);
                groupObjectPropertiesMap.put(toDraw, propertiesList);
            }
        }

        for (Map.Entry<GroupObjectInstance, List<PropertyDrawInstance>> groupObjectPropertiesEntry : groupObjectPropertiesMap.entrySet()) {
            GroupObjectInstance groupObject = groupObjectPropertiesEntry.getKey();
            List<PropertyDrawInstance> propertiesList = groupObjectPropertiesEntry.getValue();

            if (groupObject == null) { //panel properties without objects
                for (PropertyDrawInstance property : propertiesList) {
                    String integrationSID = property.getIntegrationSID();
                    if (property.isProperty() && property.toDraw == null && integrationSID != null) {
                        ObjectValue rowPropertyObject = properties.get(property).singleValue();
                        if (rowPropertyObject != null) {
                            Object rowPropertyValue = rowPropertyObject.getValue();
                            modifyJSON.put(integrationSID, rowPropertyValue != null ? rowPropertyValue : "null");
                        }
                    }
                }
            } else { //objects in grid
                JSONObject gridObjectsJSON = new JSONObject();
                JSONArray gridObjectsList = new JSONArray();

                for (ImMap<ObjectInstance, DataObject> gridObjectRow : groupObject.keys.keyIt()) {
                    JSONObject gridObjectRowJSON = new JSONObject();

                    for (PropertyDrawInstance property : propertiesList) {
                        String integrationSID = property.getIntegrationSID();
                        if (property.toDraw != null && integrationSID != null && !panelProperties.contains(property)) {
                            ObjectValue rowPropertyObject = properties.get(property).get(gridObjectRow);
                            if (rowPropertyObject != null) {
                                Object rowPropertyValue = rowPropertyObject.getValue();
                                gridObjectRowJSON.put(integrationSID, rowPropertyValue != null ? rowPropertyValue : "null");
                            }
                        }
                    }

                    if(gridObjects.containsKey(groupObject)) {
                        Object valueJSON = getClientGroupObjectValueID(gridObjectRow);
                        if (valueJSON != null) {
                            gridObjectRowJSON.put("value", valueJSON);
                        }
                    }

                    gridObjectsList.put(gridObjectRowJSON);

                }
                if(!gridObjectsList.isEmpty()) {
                    gridObjectsJSON.put("list", gridObjectsList);
                }

                //panel properties of objects in grid
                for (PropertyReaderInstance property : properties.keyIt()) {
                    if (property instanceof PropertyDrawInstance) {
                        GroupObjectInstance toDraw = ((PropertyDrawInstance) property).toDraw;
                        if (toDraw != null && toDraw.equals(groupObject) && panelProperties.contains((PropertyDrawInstance) property)) {
                            String integrationSID = ((PropertyDrawInstance) property).getIntegrationSID();
                            ObjectValue propertyObject = properties.get(property).singleValue();
                            if (((PropertyDrawInstance) property).isProperty() && integrationSID != null && propertyObject != null) {
                                Object propertyValue = propertyObject.getValue();
                                gridObjectsJSON.put(integrationSID, propertyValue != null ? propertyValue : "null");
                            }
                        }
                    }
                }

                //current objects
                Object valueJSON = getClientGroupObjectValueID(objects.get(groupObject));
                if (valueJSON != null) {
                    gridObjectsJSON.put("value", valueJSON);
                }

                modifyJSON.put(groupObject.getSID(), gridObjectsJSON);
            }
        }

        //drop properties
        JSONArray dropPropertiesArray = new JSONArray();
        for (PropertyDrawInstance dropProperty : dropProperties) {
            dropPropertiesArray.put(dropProperty.getIntegrationSID());
        }

        JSONObject response = new JSONObject();
        response.put("modify", modifyJSON);
        if(!dropPropertiesArray.isEmpty()) {
            response.put("drop", dropPropertiesArray);
        }
        return response.toString();
    }

    private Object getClientGroupObjectValueID(ImMap<ObjectInstance, ? extends ObjectValue> gridObjectRow) {
        Object result = null;
        if(gridObjectRow != null) {
            if (gridObjectRow.size() == 1) {
                result = gridObjectRow.singleValue().getValue();
            } else {
                result = new JSONObject();
                for (ObjectInstance gridObjectRowKey : gridObjectRow.keyIt()) {
                    ((JSONObject) result).put(gridObjectRowKey.getSID(), gridObjectRow.get(gridObjectRowKey).getValue());
                }
            }
        }
        return result;
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

        logger.trace("   Goes to panel ---------------");
        for (PropertyDrawInstance property : panelProperties) {
            logger.trace("     " + property);
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

        logger.trace("  CLASSVIEWS ---------------");
        for (int i=0,size=classViews.size();i<size;i++) {
            logger.trace("     " + classViews.getKey(i) + " - " + classViews.getValue(i));
        }
    }
}
