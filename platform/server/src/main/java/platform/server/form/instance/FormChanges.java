package platform.server.form.instance;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.interop.ClassViewType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static platform.base.BaseUtils.serializeObject;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    private final ImMap<GroupObjectInstance, ClassViewType> classViews;

    // value.keySet() из key.getUpTreeGroups
    private final ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects;

    // value.keySet() из key.getUpTreeGroups
    private final ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects;

    // value.keySet() из key, или пустой если root
    private final ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects;

    private final ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties;

    private final ImSet<PropertyDrawInstance> panelProperties;
    private final ImSet<PropertyDrawInstance> dropProperties;

    public FormChanges(ImMap<GroupObjectInstance, ClassViewType> classViews, ImMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects, ImMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects, ImMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects, ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties, ImSet<PropertyDrawInstance> panelProperties, ImSet<PropertyDrawInstance> dropProperties) {
        this.classViews = classViews;
        this.objects = objects;
        this.gridObjects = gridObjects;
        this.parentObjects = parentObjects;
        this.properties = properties;
        this.panelProperties = panelProperties;
        this.dropProperties = dropProperties;
    }

    void out(FormInstance<?> bv) {
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

        outStream.writeUTF(""); // обратная совместимость
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

    public void logChanges(FormInstance<?> bv, Logger logger) {
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

        logger.trace("  CLASSVIEWS ---------------");
        for (int i=0,size=classViews.size();i<size;i++) {
            logger.trace("     " + classViews.getKey(i) + " - " + classViews.getValue(i));
        }
    }
}
