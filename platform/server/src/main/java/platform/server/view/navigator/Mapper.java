package platform.server.view.navigator;

import platform.server.view.form.*;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

public class Mapper {

    public Mapper() {
    }

    public final Map<ObjectNavigator, ObjectImplement> objectMapper = new HashMap<ObjectNavigator, ObjectImplement>();

    private ObjectImplement mapObject(ObjectNavigator objKey, CustomClassView classView) {

        ObjectImplement objValue = objKey.baseClass.newObject(objKey.ID,objKey.getSID(),objKey.caption,classView,objKey.addOnTransaction);
        objectMapper.put(objKey, objValue);
        return objValue;
    }

    public ObjectImplement mapObject(ObjectNavigator navigator) {
        return objectMapper.get(navigator);
    }

    public final Map<GroupObjectNavigator, GroupObjectImplement> groupMapper = new HashMap<GroupObjectNavigator, GroupObjectImplement>();

    public GroupObjectImplement mapGroup(GroupObjectNavigator groupKey,int order,CustomClassView classView) {

        Collection<ObjectImplement> objects = new ArrayList<ObjectImplement>();
        for (ObjectNavigator object : groupKey)
            objects.add(mapObject(object,classView));

        GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID, objects, order,
                groupKey.pageSize, groupKey.initClassView, groupKey.banClassView);

        groupMapper.put(groupKey, groupValue);
        return groupValue;
    }

    private final Map<ControlObjectNavigator, ControlObjectImplement> controlMapper = new HashMap<ControlObjectNavigator, ControlObjectImplement>();

    public <P extends ControlInterface, C extends Control<P>, O extends ControlObjectImplement<P,C>> O mapControl(ControlObjectNavigator<P,C,O> propKey) {

        if (controlMapper.containsKey(propKey)) return (O) controlMapper.get(propKey);

        Map<P,PropertyObjectInterface> propertyMap = new HashMap<P, PropertyObjectInterface>();
        for(Map.Entry<P, ControlInterfaceNavigator> propertyImplement : propKey.mapping.entrySet())
            propertyMap.put(propertyImplement.getKey(),propertyImplement.getValue().doMapping(this));
        O propValue = propKey.createImplement(propertyMap);

        controlMapper.put(propKey, propValue);
        return propValue;
    }

    public <T extends ControlInterface, C extends Control<T>, I extends ControlObjectImplement<T,C>, O extends ControlObjectNavigator<T,C,I>> ControlView mapControlView(ControlViewNavigator<T,C,I,O> propKey) {
        return propKey.createView(propKey.ID, propKey.getSID(), mapControl(propKey.view), groupMapper.get(propKey.toDraw));
    }
}
