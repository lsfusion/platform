package platform.server.view.form;

import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges {

    public Map<GroupObjectImplement,Boolean> classViews = new HashMap<GroupObjectImplement, Boolean>();
    public Map<GroupObjectImplement,GroupObjectValue> objects = new HashMap<GroupObjectImplement, GroupObjectValue>();
    public Map<GroupObjectImplement, List<GroupObjectValue>> gridObjects = new HashMap<GroupObjectImplement, List<GroupObjectValue>>();
    public Map<PropertyView,Map<GroupObjectValue,Object>> gridProperties = new HashMap<PropertyView, Map<GroupObjectValue, Object>>();
    public Map<PropertyView,Object> panelProperties = new HashMap<PropertyView, Object>();
    public Set<PropertyView> dropProperties = new HashSet<PropertyView>();

    void out(RemoteForm<?> bv) {
        System.out.println(" ------- GROUPOBJECTS ---------------");
        for(GroupObjectImplement group : bv.groups) {
            List<GroupObjectValue> groupGridObjects = gridObjects.get(group);
            if(groupGridObjects!=null) {
                System.out.println(group.ID +" - Grid Changes");
                for(GroupObjectValue value : groupGridObjects)
                    System.out.println(value);
            }

            GroupObjectValue value = objects.get(group);
            if(value!=null)
                System.out.println(group.ID +" - Object Changes "+value);
        }

        System.out.println(" ------- PROPERTIES ---------------");
        System.out.println(" ------- Group ---------------");
        for(PropertyView property : gridProperties.keySet()) {
            Map<GroupObjectValue,Object> propertyValues = gridProperties.get(property);
            System.out.println(property+" ---- property");
            for(GroupObjectValue gov : propertyValues.keySet())
                System.out.println(gov+" - "+propertyValues.get(gov));
        }

        System.out.println(" ------- Panel ---------------");
        for(PropertyView property : panelProperties.keySet())
            System.out.println(property+" - "+ panelProperties.get(property));

        System.out.println(" ------- Drop ---------------");
        for(PropertyView property : dropProperties)
            System.out.println(property);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeInt(classViews.size());
        for (Map.Entry<GroupObjectImplement,Boolean> classView : classViews.entrySet()) {
            outStream.writeInt(classView.getKey().ID);
            outStream.writeBoolean(classView.getValue());
        }

        outStream.writeInt(objects.size());
        for (Map.Entry<GroupObjectImplement,GroupObjectValue> objectValue : objects.entrySet()) {
            outStream.writeInt(objectValue.getKey().ID);
            for (ObjectImplement object : objectValue.getKey()) // именно так чтобы гарантировано в том же порядке
                outStream.writeInt(objectValue.getValue().get(object));
        }

        outStream.writeInt(gridObjects.size());
        for (Map.Entry<GroupObjectImplement,List<GroupObjectValue>> gridObject : gridObjects.entrySet()) {
            outStream.writeInt(gridObject.getKey().ID);

            outStream.writeInt(gridObject.getValue().size());
            for (GroupObjectValue groupObjectValue : gridObject.getValue())
                for (ObjectImplement object : gridObject.getKey()) // именно так чтобы гарантировано в том же порядке
                    outStream.writeInt(groupObjectValue.get(object));
        }

        outStream.writeInt(gridProperties.size());
        for (Map.Entry<PropertyView,Map<GroupObjectValue,Object>> gridProperty : gridProperties.entrySet()) {
            outStream.writeInt(gridProperty.getKey().ID);

            outStream.writeInt(gridProperty.getValue().size());
            for (Map.Entry<GroupObjectValue,Object> gridPropertyValue : gridProperty.getValue().entrySet()) {
                for (ObjectImplement object : gridProperty.getKey().toDraw) // именно так чтобы гарантировано в том же порядке
                    outStream.writeInt(gridPropertyValue.getKey().get(object));
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
    }
}
