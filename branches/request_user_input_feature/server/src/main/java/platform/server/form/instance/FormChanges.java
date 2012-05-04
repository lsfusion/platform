package platform.server.form.instance;

import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static platform.base.BaseUtils.serializeObject;

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

    public Map<PropertyReaderInstance, Map<Map<ObjectInstance, DataObject>, ObjectValue>> properties = new HashMap<PropertyReaderInstance, Map<Map<ObjectInstance, DataObject>, ObjectValue>>();

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
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
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

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (Map.Entry<GroupObjectInstance, ClassViewType> classView : classViews.entrySet()) {
            outStream.writeInt(classView.getKey().getID());
            outStream.writeInt(classView.getValue().ordinal());
        }

        outStream.writeInt(objects.size());
        for (Map.Entry<GroupObjectInstance, Map<ObjectInstance, ? extends ObjectValue>> objectValue : objects.entrySet()) {
            outStream.writeInt(objectValue.getKey().getID());
            serializeGroupObjectValue(outStream, objectValue.getValue());
        }

        serializeKeyObjectsMap(outStream, gridObjects);
        serializeKeyObjectsMap(outStream, parentObjects);

        outStream.writeInt(panelProperties.size());
        for (PropertyReaderInstance propertyReader : panelProperties) {
            outStream.writeByte(propertyReader.getTypeID());
            outStream.writeInt(propertyReader.getID());
        }

        outStream.writeInt(properties.size());
        for (Map.Entry<PropertyReaderInstance, Map<Map<ObjectInstance, DataObject>, ObjectValue>> gridProperty : properties.entrySet()) {
            PropertyReaderInstance propertyReadInstance = gridProperty.getKey();

            // сериализация PropertyReadInterface
            outStream.writeByte(propertyReadInstance.getTypeID());
            outStream.writeInt(propertyReadInstance.getID());

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> gridPropertyValue : gridProperty.getValue().entrySet()) {
                Map<ObjectInstance, DataObject> objectValues = gridPropertyValue.getKey();

                serializeGroupObjectValue(outStream, objectValues);

                serializeObject(outStream, gridPropertyValue.getValue().getValue());
            }
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties) {
            outStream.writeInt(propertyView.getID());
        }

        outStream.writeUTF(message);

        serializeObject(outStream, dataChanged);
    }

    private void serializeGroupObjectValue(DataOutputStream outStream, Map<ObjectInstance,? extends ObjectValue> values) throws IOException {
        outStream.writeInt(values.size());
        for (Map.Entry<ObjectInstance, ? extends ObjectValue> entry : values.entrySet()) {
            outStream.writeInt(entry.getKey().getID());
            serializeObject(outStream, entry.getValue().getValue());
        }
    }

    private void serializeKeyObjectsMap(DataOutputStream outStream, Map<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> keyObjects) throws IOException {
        outStream.writeInt(keyObjects.size());
        for (Map.Entry<GroupObjectInstance, List<Map<ObjectInstance, DataObject>>> gridObject : keyObjects.entrySet()) {

            outStream.writeInt(gridObject.getKey().getID());

            outStream.writeInt(gridObject.getValue().size());
            for (Map<ObjectInstance, DataObject> groupObjectValue : gridObject.getValue()) {
                serializeGroupObjectValue(outStream, groupObjectValue);
            }
        }
    }

    public void logChanges(FormInstance<?> bv, Logger logger) {
        logger.debug("getFormChanges:");
        logger.debug("  GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.groups) {
            List<Map<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                logger.debug("   " + group.getID() + " - Current grid objects chaned to:");
                for (Map<ObjectInstance, DataObject> value : groupGridObjects)
                    logger.debug("     " + value);
            }

            Map<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null) {
                logger.debug("   " + group.getID() + " - Current object changed to:  " + value);
            }
        }

        logger.debug("  PROPERTIES ---------------");
        logger.debug("   Values ---------------");
        for (PropertyReaderInstance property : properties.keySet()) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            logger.debug("    " + property + " ---- property");
            for (Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> propValue : propertyValues.entrySet())
                logger.debug("      " + propValue.getKey() + " -> " + propValue.getValue());
        }

        logger.debug("   Goes to panel ---------------");
        for (PropertyReaderInstance property : panelProperties) {
            logger.debug("     " + property);
        }

        logger.debug("   Droped ---------------");
        for (PropertyDrawInstance property : dropProperties)
            logger.debug("     " + property);

        logger.debug("  CLASSVIEWS ---------------");
        for (Map.Entry<GroupObjectInstance, ClassViewType> classView : classViews.entrySet()) {
            logger.debug("     " + classView.getKey() + " - " + classView.getValue());
        }
    }
}
