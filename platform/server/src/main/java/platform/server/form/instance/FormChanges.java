package platform.server.form.instance;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static platform.base.BaseUtils.serializeObject;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

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

    public byte[] serialize() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serialize(new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void logChanges(FormInstance<?> bv, Logger logger) {
        logger.trace("getFormChanges:");
        logger.trace("  GROUPOBJECTS ---------------");
        for (GroupObjectInstance group : bv.groups) {
            List<Map<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if (groupGridObjects != null) {
                logger.trace("   " + group.getID() + " - Current grid objects chaned to:");
                for (Map<ObjectInstance, DataObject> value : groupGridObjects)
                    logger.trace("     " + value);
            }

            Map<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if (value != null) {
                logger.trace("   " + group.getID() + " - Current object changed to:  " + value);
            }
        }

        logger.trace("  PROPERTIES ---------------");
        logger.trace("   Values ---------------");
        for (PropertyReaderInstance property : properties.keySet()) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = properties.get(property);
            logger.trace("    " + property + " ---- property");
            for (Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> propValue : propertyValues.entrySet())
                logger.trace("      " + propValue.getKey() + " -> " + propValue.getValue());
        }

        logger.trace("   Goes to panel ---------------");
        for (PropertyReaderInstance property : panelProperties) {
            logger.trace("     " + property);
        }

        logger.trace("   Droped ---------------");
        for (PropertyDrawInstance property : dropProperties)
            logger.trace("     " + property);

        logger.trace("  CLASSVIEWS ---------------");
        for (Map.Entry<GroupObjectInstance, ClassViewType> classView : classViews.entrySet()) {
            logger.trace("     " + classView.getKey() + " - " + classView.getValue());
        }
    }
}
