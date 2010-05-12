package platform.server.view.form;

import platform.base.BaseUtils;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.classes.ConcreteValueClass;

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
    public Map<ControlView,Map<Map<ObjectImplement,DataObject>,Object>> gridControls = new HashMap<ControlView, Map<Map<ObjectImplement, DataObject>, Object>>();
    public Map<ControlView,Object> panelControls = new HashMap<ControlView, Object>();
    public Set<ControlView> dropProperties = new HashSet<ControlView>();

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
        for(ControlView property : gridControls.keySet()) {
            Map<Map<ObjectImplement, DataObject>, Object> propertyValues = gridControls.get(property);
            System.out.println(property+" ---- control");
            for(Map<ObjectImplement, DataObject> gov : propertyValues.keySet())
                System.out.println(gov+" - "+propertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(ControlView property : panelControls.keySet())
            System.out.println(property+" - "+ panelControls.get(property));

        System.out.println(" ------- Drop ---------------");
        for(ControlView property : dropProperties)
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
            for (ObjectImplement object : objectValue.getKey().objects) { // именно так чтобы гарантировано в том же порядке
                BaseUtils.serializeObject(outStream,objectValue.getValue().get(object).getValue());
            }
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
                    outStream.writeInt((Integer)groupObjectValue.get(object).object);
        }

        outStream.writeInt(gridClasses.size());
        for (Map.Entry<GroupObjectImplement,List<Map<ObjectImplement,ConcreteValueClass>>> gridClass : gridClasses.entrySet()) {

            outStream.writeInt(gridClass.getKey().ID);

            outStream.writeInt(gridClass.getValue().size());
            for (Map<ObjectImplement, ConcreteValueClass> groupObjectValue : gridClass.getValue())
                for (ObjectImplement object : gridClass.getKey().objects) // именно так чтобы гарантировано в том же порядке
                    groupObjectValue.get(object).serialize(outStream);
        }

        outStream.writeInt(gridControls.size());
        for (Map.Entry<ControlView,Map<Map<ObjectImplement,DataObject>,Object>> gridProperty : gridControls.entrySet()) {
            outStream.writeInt(gridProperty.getKey().ID);

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<Map<ObjectImplement,DataObject>,Object> gridPropertyValue : gridProperty.getValue().entrySet()) {
                for (ObjectImplement object : gridProperty.getKey().toDraw.objects) // именно так чтобы гарантировано в том же порядке
                    outStream.writeInt((Integer) gridPropertyValue.getKey().get(object).getValue());
                BaseUtils.serializeObject(outStream, gridPropertyValue.getValue());
            }
        }

        outStream.writeInt(panelControls.size());
        for (Map.Entry<ControlView,Object> panelProperty : panelControls.entrySet()) {
            outStream.writeInt(panelProperty.getKey().ID);

            BaseUtils.serializeObject(outStream, panelProperty.getValue());
        }

        outStream.writeInt(dropProperties.size());
        for (ControlView propertyView : dropProperties)
            outStream.writeInt(propertyView.ID);

        outStream.writeUTF(message);
    }
}
