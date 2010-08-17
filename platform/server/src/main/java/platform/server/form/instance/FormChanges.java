package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.server.classes.ConcreteValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    Boolean dataChanged = null;

    public String message = "";

    public Map<GroupObjectInstance,Byte> classViews = new HashMap<GroupObjectInstance, Byte>();
    public Map<GroupObjectInstance,Map<ObjectInstance,? extends ObjectValue>> objects = new HashMap<GroupObjectInstance,Map<ObjectInstance,? extends ObjectValue>>();
    public Map<GroupObjectInstance,Map<ObjectInstance,ConcreteValueClass>> classes = new HashMap<GroupObjectInstance,Map<ObjectInstance,ConcreteValueClass>>();
    public Map<GroupObjectInstance,List<Map<ObjectInstance,DataObject>>> gridObjects = new HashMap<GroupObjectInstance,List<Map<ObjectInstance,DataObject>>>();
    public Map<GroupObjectInstance,List<Map<ObjectInstance,ConcreteValueClass>>> gridClasses = new HashMap<GroupObjectInstance,List<Map<ObjectInstance,ConcreteValueClass>>>();
    public Map<PropertyDrawInstance,Map<Map<ObjectInstance,DataObject>,Object>> gridProperties = new HashMap<PropertyDrawInstance, Map<Map<ObjectInstance, DataObject>, Object>>();
    public Map<PropertyDrawInstance,Object> panelProperties = new HashMap<PropertyDrawInstance, Object>();
    public Set<PropertyDrawInstance> dropProperties = new HashSet<PropertyDrawInstance>();

    void out(FormInstance<?> bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectInstance group : bv.groups) {
            List<Map<ObjectInstance, DataObject>> groupGridObjects = gridObjects.get(group);
            if(groupGridObjects!=null) {
                System.out.println(group.getID() +" - Grid Changes");
                for(Map<ObjectInstance, DataObject> value : groupGridObjects)
                    System.out.println(value);
            }

            Map<ObjectInstance, ? extends ObjectValue> value = objects.get(group);
            if(value!=null)
                System.out.println(group.getID() +" - Object Changes "+value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyDrawInstance property : gridProperties.keySet()) {
            Map<Map<ObjectInstance, DataObject>, Object> propertyValues = gridProperties.get(property);
            System.out.println(property+" ---- property");
            for(Map<ObjectInstance, DataObject> gov : propertyValues.keySet())
                System.out.println(gov+" - "+propertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyDrawInstance property : panelProperties.keySet())
            System.out.println(property+" - "+ panelProperties.get(property));

        System.out.println(" ------- Drop ---------------");
        for(PropertyDrawInstance property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- CLASSVIEWS ---------------");
        for (Map.Entry<GroupObjectInstance,Byte> classView : classViews.entrySet()) {
            System.out.println(classView.getKey() + " - " + classView.getValue());
        }

    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (Map.Entry<GroupObjectInstance,Byte> classView : classViews.entrySet()) {
            outStream.writeInt(classView.getKey().getID());
            outStream.writeByte(classView.getValue());
        }

        outStream.writeInt(objects.size());
        for (Map.Entry<GroupObjectInstance,Map<ObjectInstance,? extends ObjectValue>> objectValue : objects.entrySet()) {
            outStream.writeInt(objectValue.getKey().getID());
            for (ObjectInstance object : objectValue.getKey().objects) // именно так чтобы гарантировано в том же порядке
                BaseUtils.serializeObject(outStream,objectValue.getValue().get(object).getValue());
        }

        outStream.writeInt(classes.size()); // количество элементов в classes может быть меньше, поскольку в objects могут быть null'ы
        for (Map.Entry<GroupObjectInstance,Map<ObjectInstance,ConcreteValueClass>> classValue : classes.entrySet()) {
            outStream.writeInt(classValue.getKey().getID());
            for (ObjectInstance object : classValue.getKey().objects) { // именно так чтобы гарантировано в том же порядке
                ConcreteValueClass cls = classValue.getValue().get(object);
                if (cls == null) {
                    outStream.writeBoolean(true);
                } else {
                    outStream.writeBoolean(false);
                    cls.serialize(outStream);
                }
            }
        }

        outStream.writeInt(gridObjects.size());
        for (Map.Entry<GroupObjectInstance,List<Map<ObjectInstance,DataObject>>> gridObject : gridObjects.entrySet()) {

            outStream.writeInt(gridObject.getKey().getID());

            outStream.writeInt(gridObject.getValue().size());
            for (Map<ObjectInstance, DataObject> groupObjectValue : gridObject.getValue())
                for (ObjectInstance object : gridObject.getKey().objects) // именно так чтобы гарантировано в том же порядке
                    BaseUtils.serializeObject(outStream,groupObjectValue.get(object).object);
        }

        outStream.writeInt(gridClasses.size());
        for (Map.Entry<GroupObjectInstance,List<Map<ObjectInstance,ConcreteValueClass>>> gridClass : gridClasses.entrySet()) {

            outStream.writeInt(gridClass.getKey().getID());

            outStream.writeInt(gridClass.getValue().size());
            for (Map<ObjectInstance, ConcreteValueClass> groupObjectValue : gridClass.getValue())
                for (ObjectInstance object : gridClass.getKey().objects) // именно так чтобы гарантировано в том же порядке
                    groupObjectValue.get(object).serialize(outStream);
        }

        outStream.writeInt(gridProperties.size());
        for (Map.Entry<PropertyDrawInstance,Map<Map<ObjectInstance,DataObject>,Object>> gridProperty : gridProperties.entrySet()) {
            outStream.writeInt(gridProperty.getKey().getID());

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<Map<ObjectInstance,DataObject>,Object> gridPropertyValue : gridProperty.getValue().entrySet()) {
                for (ObjectInstance object : gridProperty.getKey().toDraw.objects) // именно так чтобы гарантировано в том же порядке
                    BaseUtils.serializeObject(outStream,gridPropertyValue.getKey().get(object).getValue());
                BaseUtils.serializeObject(outStream, gridPropertyValue.getValue());
            }
        }

        outStream.writeInt(panelProperties.size());
        for (Map.Entry<PropertyDrawInstance,Object> panelProperty : panelProperties.entrySet()) {
            outStream.writeInt(panelProperty.getKey().getID());

            BaseUtils.serializeObject(outStream, panelProperty.getValue());
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyDrawInstance propertyView : dropProperties)
            outStream.writeInt(propertyView.getID());

        outStream.writeUTF(message);

        BaseUtils.serializeObject(outStream, dataChanged);
    }
}
