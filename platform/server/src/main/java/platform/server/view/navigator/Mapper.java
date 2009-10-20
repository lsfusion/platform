package platform.server.view.navigator;

import platform.server.view.form.*;
import platform.server.logics.properties.PropertyInterface;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
* User: ME2
* Date: 20.10.2009
* Time: 11:41:06
* To change this template use File | Settings | File Templates.
*/
public class Mapper {

    public Mapper() {
    }

    public final Map<ObjectNavigator, ObjectImplement> objectMapper = new HashMap<ObjectNavigator, ObjectImplement>();

    private ObjectImplement mapObject(ObjectNavigator objKey, CustomClassView classView) {

        ObjectImplement objValue = objKey.baseClass.newObject(objKey.ID,objKey.getSID(),objKey.caption,classView);
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

        GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID,objects,order,
                groupKey.pageSize,groupKey.gridClassView,groupKey.singleViewType);

        groupMapper.put(groupKey, groupValue);
        return groupValue;
    }

    private final Map<PropertyObjectNavigator, PropertyObjectImplement> propertyMapper = new HashMap<PropertyObjectNavigator, PropertyObjectImplement>();

    public <P extends PropertyInterface> PropertyObjectImplement<P> mapProperty(PropertyObjectNavigator<P> propKey) {

        if (propertyMapper.containsKey(propKey)) return propertyMapper.get(propKey);

        Map<P,PropertyObjectInterface> propertyMap = new HashMap<P, PropertyObjectInterface>();
        for(Map.Entry<P,PropertyInterfaceNavigator> propertyImplement : propKey.mapping.entrySet())
            propertyMap.put(propertyImplement.getKey(),propertyImplement.getValue().doMapping(this));
        PropertyObjectImplement<P> propValue = new PropertyObjectImplement<P>(propKey.property,propertyMap);

        propertyMapper.put(propKey, propValue);
        return propValue;
    }

    public <P extends PropertyInterface> PropertyView mapPropertyView(PropertyViewNavigator<P> propKey) {
        return new PropertyView<P>(propKey.ID, propKey.getSID(), mapProperty(propKey.view), groupMapper.get(propKey.toDraw));
    }
}
