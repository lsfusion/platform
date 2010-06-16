package platform.server.view.navigator;

import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Mapper {

    public final PropertyObjectInterface user;
    public final PropertyObjectInterface computer;

    public Mapper(PropertyObjectInterface user, PropertyObjectInterface computer) {
        this.user = user;
        this.computer = computer;
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

    private final Map<PropertyObjectNavigator, PropertyObjectImplement> propertyMapper = new HashMap<PropertyObjectNavigator, PropertyObjectImplement>();

    public <P extends PropertyInterface> PropertyObjectImplement mapProperty(PropertyObjectNavigator<P> propKey) {

        if (propertyMapper.containsKey(propKey)) return propertyMapper.get(propKey);

        Map<P,PropertyObjectInterface> propertyMap = new HashMap<P, PropertyObjectInterface>();
        for(Map.Entry<P, PropertyInterfaceNavigator> propertyImplement : propKey.mapping.entrySet())
            propertyMap.put(propertyImplement.getKey(),propertyImplement.getValue().doMapping(this));
        PropertyObjectImplement propValue = new PropertyObjectImplement<P>(propKey.property, propertyMap);

        propertyMapper.put(propKey, propValue);
        return propValue;
    }

    private final Map<PropertyViewNavigator, PropertyView> propertyViewMapper = new HashMap<PropertyViewNavigator, PropertyView>();

    public <T extends PropertyInterface> PropertyView mapPropertyView(PropertyViewNavigator<T> propKey) {

        if (propertyViewMapper.containsKey(propKey)) return propertyViewMapper.get(propKey);

        PropertyView propValue = new PropertyView<T>(propKey.ID, propKey.getSID(), mapProperty(propKey.view), groupMapper.get(propKey.toDraw), propKey.forcePanel);
        
        propertyViewMapper.put(propKey, propValue);
        return propValue;
    }
}
