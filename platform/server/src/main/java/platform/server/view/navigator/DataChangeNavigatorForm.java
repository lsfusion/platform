package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.session.PropertyChange;

public class DataChangeNavigatorForm<T extends BusinessLogics<T>> extends NavigatorForm<T> {

//    final GroupObjectNavigator interfaceGroup;
//    final Map<DataPropertyInterface,ObjectNavigator> interfaceObjects;

    final ObjectNavigator valueObject;
    final GroupObjectNavigator valueGroup;

    public DataChangeNavigatorForm(T BL, PropertyChange<?,?> change) {
        super(54555+change.property.ID, change.toString());

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
        valueObject = new ObjectNavigator(IDShift(1), change.getDialogClass(), "Код");
        valueGroup.add(valueObject);
        addGroup(valueGroup);

        for(FilterNavigator filter : change.getFilters(valueObject, BL))
            addFixedFilter(filter);

        addPropertyView(BL.properties, BL.baseGroup, true, valueObject);
        addPropertyView(BL.properties, BL.aggrGroup, true, valueObject);
    }

/*    public <T extends BusinessLogics<T>> void seekObjects(RemoteForm<T> remoteForm, Mapper mapper, Object dataValue, Map<DataPropertyInterface, Object> interfaceValues) {
//        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(interfaceGroup),BaseUtils.crossJoin(BaseUtils.join(interfaceObjects,mapper.objectMapper),interfaceValues));
        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(valueGroup),Collections.singletonMap(mapper.objectMapper.get(valueObject),dataValue));
    }*/
}

