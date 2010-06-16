package platform.server.view.form;

import platform.base.BaseUtils;
import platform.server.classes.ConcreteValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    public String message = "";

    public Map<GroupObjectImplement,Byte> classViews = new HashMap<GroupObjectImplement, Byte>();
    public Map<GroupObjectImplement,Map<ObjectImplement,? extends ObjectValue>> objects = new HashMap<GroupObjectImplement,Map<ObjectImplement,? extends ObjectValue>>();
    public Map<GroupObjectImplement,Map<ObjectImplement,ConcreteValueClass>> classes = new HashMap<GroupObjectImplement,Map<ObjectImplement,ConcreteValueClass>>();
    public Map<GroupObjectImplement,List<Map<ObjectImplement,DataObject>>> gridObjects = new HashMap<GroupObjectImplement,List<Map<ObjectImplement,DataObject>>>();
    public Map<GroupObjectImplement,List<Map<ObjectImplement,ConcreteValueClass>>> gridClasses = new HashMap<GroupObjectImplement,List<Map<ObjectImplement,ConcreteValueClass>>>();
    public Map<PropertyView,Map<Map<ObjectImplement,DataObject>,Object>> gridProperties = new HashMap<PropertyView, Map<Map<ObjectImplement, DataObject>, Object>>();
    public Map<PropertyView,Object> panelProperties = new HashMap<PropertyView, Object>();
    public Set<PropertyView> dropProperties = new HashSet<PropertyView>();

    void out(RemoteForm<?> bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement group : bv.groups) {
            List<Map<ObjectImplement, DataObject>> groupGridObjects = gridObjects.get(group);
            if(groupGridObjects!=null) {
                System.out.println(group.ID +" - Grid Changes");
                for(Map<ObjectImplement, DataObject> value : groupGridObjects)
                    System.out.println(value);
            }

            Map<ObjectImplement, ? extends ObjectValue> value = objects.get(group);
            if(value!=null)
                System.out.println(group.ID +" - Object Changes "+value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyView property : gridProperties.keySet()) {
            Map<Map<ObjectImplement, DataObject>, Object> propertyValues = gridProperties.get(property);
            System.out.println(property+" ---- property");
            for(Map<ObjectImplement, DataObject> gov : propertyValues.keySet())
                System.out.println(gov+" - "+propertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyView property : panelProperties.keySet())
            System.out.println(property+" - "+ panelProperties.get(property));

        System.out.println(" ------- Drop ---------------");
        for(PropertyView property : dropProperties)
            System.out.println(property);

        System.out.println(" ------- CLASSVIEWS ---------------");
        for (Map.Entry<GroupObjectImplement,Byte> classView : classViews.entrySet()) {
            System.out.println(classView.getKey() + " - " + classView.getValue());
        }

    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (Map.Entry<GroupObjectImplement,Byte> classView : classViews.entrySet()) {
            outStream.writeInt(classView.getKey().ID);
            outStream.writeByte(classView.getValue());
        }

        outStream.writeInt(objects.size());
        for (Map.Entry<GroupObjectImplement,Map<ObjectImplement,? extends ObjectValue>> objectValue : objects.entrySet()) {
            outStream.writeInt(objectValue.getKey().ID);
            for (ObjectImplement object : objectValue.getKey().objects) // именно так чтобы гарантировано в том же порядке
                BaseUtils.serializeObject(outStream,objectValue.getValue().get(object).getValue());
        }

        outStream.writeInt(classes.size()); // количество элементов в classes может быть меньше, поскольку в objects могут быть null'ы
        for (Map.Entry<GroupObjectImplement,Map<ObjectImplement,ConcreteValueClass>> classValue : classes.entrySet()) {
            outStream.writeInt(classValue.getKey().ID);
            for (ObjectImplement object : classValue.getKey().objects) { // именно так чтобы гарантировано в том же порядке
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
        for (Map.Entry<GroupObjectImplement,List<Map<ObjectImplement,DataObject>>> gridObject : gridObjects.entrySet()) {

            outStream.writeInt(gridObject.getKey().ID);

            outStream.writeInt(gridObject.getValue().size());
            for (Map<ObjectImplement, DataObject> groupObjectValue : gridObject.getValue())
                for (ObjectImplement object : gridObject.getKey().objects) // именно так чтобы гарантировано в том же порядке
                    BaseUtils.serializeObject(outStream,groupObjectValue.get(object).object);
        }

        outStream.writeInt(gridClasses.size());
        for (Map.Entry<GroupObjectImplement,List<Map<ObjectImplement,ConcreteValueClass>>> gridClass : gridClasses.entrySet()) {

            outStream.writeInt(gridClass.getKey().ID);

            outStream.writeInt(gridClass.getValue().size());
            for (Map<ObjectImplement, ConcreteValueClass> groupObjectValue : gridClass.getValue())
                for (ObjectImplement object : gridClass.getKey().objects) // именно так чтобы гарантировано в том же порядке
                    groupObjectValue.get(object).serialize(outStream);
        }

        outStream.writeInt(gridProperties.size());
        for (Map.Entry<PropertyView,Map<Map<ObjectImplement,DataObject>,Object>> gridProperty : gridProperties.entrySet()) {
            outStream.writeInt(gridProperty.getKey().ID);

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<Map<ObjectImplement,DataObject>,Object> gridPropertyValue : gridProperty.getValue().entrySet()) {
                for (ObjectImplement object : gridProperty.getKey().toDraw.objects) // именно так чтобы гарантировано в том же порядке
                    BaseUtils.serializeObject(outStream,gridPropertyValue.getKey().get(object).getValue());
                BaseUtils.serializeObject(outStream, gridPropertyValue.getValue());
            }
        }

        outStream.writeInt(panelProperties.size());
        for (Map.Entry<PropertyView,Object> panelProperty : panelProperties.entrySet()) {
            outStream.writeInt(panelProperty.getKey().ID);

            BaseUtils.serializeObject(outStream, panelProperty.getValue());
        }

        outStream.writeInt(dropProperties.size());
        for (PropertyView propertyView : dropProperties)
            outStream.writeInt(propertyView.ID);

        outStream.writeUTF(message);
    }
}
