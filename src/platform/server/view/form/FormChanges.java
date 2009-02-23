package platform.server.view.form;

import platform.interop.AbstractFormChanges;

import java.util.List;
import java.util.Map;

// появляется по сути для отделения клиента, именно он возвращается назад клиенту
public class FormChanges extends AbstractFormChanges<GroupObjectImplement,GroupObjectValue, PropertyView> {

    void Out(RemoteForm<?> bv) {
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
}
