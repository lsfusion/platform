package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.logics.properties.Property;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.view.navigator.filter.NotFilterNavigator;

import java.util.Map;
import java.util.HashMap;

public class DataChangeNavigatorForm<T extends BusinessLogics<T>> extends NavigatorForm<T> {

//    final GroupObjectNavigator interfaceGroup;
//    final Map<DataPropertyInterface,ObjectNavigator> interfaceObjects;

    final ObjectNavigator valueObject;
    final GroupObjectNavigator valueGroup;

    public DataChangeNavigatorForm(T BL, DataProperty change, Map<DataPropertyInterface, DataObject> interfaceValues) {
        super(change.ID + 54555, change.caption);

/*        // добавляем элементы для которых меняем на форму
        interfaceGroup = new GroupObjectNavigator(IDShift(1));
        interfaceGroup.singleViewType = true; interfaceGroup.gridClassView = false;
        interfaceObjects = new HashMap<DataPropertyInterface, ObjectNavigator>();
        for(DataPropertyInterface propertyInterface : change.interfaces) {
            ObjectNavigator interfaceObject = new ObjectNavigator(propertyInterface.ID,propertyInterface.interfaceClass,propertyInterface.toString());
            interfaceObject.show = false;
            interfaceObjects.put(propertyInterface,interfaceObject);
            interfaceGroup.add(interfaceObject);
        }
        addGroup(interfaceGroup);*/

        // сам объект который ищем вешаем на форму
        valueGroup = new GroupObjectNavigator(IDShift(1));
        valueObject = new ObjectNavigator(change.ID, change.value, change.toString());
        valueGroup.add(valueObject);
        addGroup(valueGroup);

        Map<DataPropertyInterface, PropertyInterfaceNavigator> mapObjects = new HashMap<DataPropertyInterface, PropertyInterfaceNavigator>(interfaceValues);
        mapObjects.put(change.valueInterface,valueObject);
        for(Property property : BL.getChangeConstrainedProperties(change)) // добавляем все констрейнты
            addFixedFilter(new NotFilterNavigator(new NotNullFilterNavigator<DataPropertyInterface>(
                    new PropertyObjectNavigator<DataPropertyInterface>(property,mapObjects))));

        addPropertyView(BL.properties, BL.baseGroup, true, valueObject);
        addPropertyView(BL.properties, BL.aggrGroup, true, valueObject);
    }

/*    public <T extends BusinessLogics<T>> void seekObjects(RemoteForm<T> remoteForm, Mapper mapper, Object dataValue, Map<DataPropertyInterface, Object> interfaceValues) {
//        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(interfaceGroup),BaseUtils.crossJoin(BaseUtils.join(interfaceObjects,mapper.objectMapper),interfaceValues));
        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(valueGroup),Collections.singletonMap(mapper.objectMapper.get(valueObject),dataValue));
    }*/
}

