package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    Boolean dataChanged = null;

    public String message = "";

    public Map<GroupObjectInstance, ClassViewType> classViews = new HashMap<GroupObjectInstance, ClassViewType>();

    // value.keySet() из key.getUpTreeGroups
    public Map<GroupObjectInstance, Map<ObjectInstance, ? extends ObjectValue>> objects = new HashMap<GroupObjectInstance, Map<ObjectInstance, ? extends ObjectValue>>();

    // value.keySet() из key.getUpTreeGroups
    public Map<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> gridObjects = new HashMap<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>>();

    // value.keySet() из key, или пустой если root
    public Map<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> parentObjects = new HashMap<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>>();

    public Map<PropertyReaderInstance, Map<Map<ObjectInstance, DataObject>, Object>> properties = new HashMap<PropertyReaderInstance, Map<Map<ObjectInstance, DataObject>, Object>>();

    public Set<PropertyReaderInstance> panelProperties = new HashSet<PropertyReaderInstance>();
    public Set<PropertyDrawInstance> dropProperties = new HashSet<PropertyDrawInstance>();

    void out(FormInstance<?> bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.groups) {
            List<Map<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                System.out.println(group.getID() + " - GRID Changes");
                for (Map<ObjectInstance, DataObject> value : groupGridObjects)
                    System.out.println(value);
            }

            Map<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null)
                System.out.println(group.getID() + " - Object Changes " + value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for (PropertyReaderInstance property : properties.keySet()) {
            Map<Map<ObjectInstance, DataObject>, Object> propertyValues = properties.get(property);
            System.out.println(property + " ---- property");
            for (Map<ObjectInstance, DataObject> gov : propertyValues.keySet())
                System.out.println(gov + " - " + propertyValues.get(gov));
        }

        System.out.println(" ------- PANEL ---------------");
        for (PropertyReaderInstance property : panelProperties)
            System.out.println(property);

        System.out.println(" ------- Drop ---------------");
        for (PropertyDrawInstance property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- CLASSVIEWS ---------------");
        for (Map.Entry<GroupObjectInstance, ClassViewType> classView : classViews.entrySet()) {
            System.out.println(classView.getKey() + " - " + classView.getValue());
        }

    }

    private void serializeGroupObjectValue(DataOutputStream outStream, GroupObjectInstance groupObject, Map<ObjectInstance,? extends ObjectValue> values) throws IOException {
        // именно так чтобы гарантировано в том же порядке
        for (ObjectInstance object : GroupObjectInstance.getObjects(groupObject.getUpTreeGroups())) {
            BaseUtils.serializeObject(outStream, values.get(object).getValue());
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (Map.Entry<GroupObjectInstance, ClassViewType> classView : classViews.entrySet()) {
            outStream.writeInt(classView.getKey().getID());
            outStream.writeInt(classView.getValue().ordinal());
        }

        outStream.writeInt(objects.size());
        for (Map.Entry<GroupObjectInstance, Map<ObjectInstance, ? extends ObjectValue>> objectValue : objects.entrySet()) {
            outStream.writeInt(objectValue.getKey().getID());
            serializeGroupObjectValue(outStream, objectValue.getKey(), objectValue.getValue());
        }

        serializeKeyObjectsMap(outStream, gridObjects, false);
        serializeKeyObjectsMap(outStream, parentObjects, true);

        outStream.writeInt(panelProperties.size());
        for (PropertyReaderInstance propertyReader : panelProperties) {
            outStream.writeByte(propertyReader.getTypeID());
            outStream.writeInt(propertyReader.getID());
        }

        outStream.writeInt(properties.size());
        for (Map.Entry<PropertyReaderInstance,Map<Map<ObjectInstance,DataObject>,Object>> gridProperty : properties.entrySet()) {
            PropertyReaderInstance propertyReadInstance = gridProperty.getKey();

            // сериализация PropertyReadInterface
            outStream.writeByte(propertyReadInstance.getTypeID());
            outStream.writeInt(propertyReadInstance.getID());

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<Map<ObjectInstance, DataObject>, Object> gridPropertyValue : gridProperty.getValue().entrySet()) {
                Map<ObjectInstance, DataObject> objectValues = gridPropertyValue.getKey();

                // именно так чтобы гарантировано в том же порядке
                for (ObjectInstance object : propertyReadInstance.getKeysObjectsList(panelProperties)) {
                    BaseUtils.serializeObject(outStream, objectValues.get(object).getValue());
                }

                BaseUtils.serializeObject(outStream, gridPropertyValue.getValue());
            }
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties) {
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeUTF(message);

        BaseUtils.serializeObject(outStream, dataChanged);
    }

    private void serializeKeyObjectsMap(DataOutputStream outStream, Map<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> keyObjects, boolean parents) throws IOException {
        outStream.writeInt(keyObjects.size());
        for (Map.Entry<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> gridObject : keyObjects.entrySet()) {

            outStream.writeInt(gridObject.getKey().getID());

            outStream.writeInt(gridObject.getValue().size());
            for (Map<ObjectInstance, DataObject> groupObjectValue : gridObject.getValue()) {
                // именно так чтобы гарантировано в том же порядке
                if(parents) {
                    if(groupObjectValue.isEmpty())
                        outStream.writeBoolean(true);
                    else {
                        outStream.writeBoolean(false);
                        for (ObjectInstance object : gridObject.getKey().objects) {
                            BaseUtils.serializeObject(outStream, groupObjectValue.get(object).getValue());
                        }
                    }
                } else
                    serializeGroupObjectValue(outStream, gridObject.getKey(), groupObjectValue);
            }
        }
    }
}
