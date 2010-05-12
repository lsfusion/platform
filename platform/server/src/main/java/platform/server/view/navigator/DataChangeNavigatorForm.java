package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.view.navigator.filter.NotFilterNavigator;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.classes.CustomClass;

public class DataChangeNavigatorForm<T extends BusinessLogics<T>> extends NavigatorForm<T> {

//    final GroupObjectNavigator interfaceGroup;
//    final Map<ClassPropertyInterface,ObjectNavigator> interfaceObjects;

    final ObjectNavigator valueObject;
    final GroupObjectNavigator valueGroup;

    public <P extends PropertyInterface> DataChangeNavigatorForm(T BL, PropertyValueImplement<P> implement, CustomClass changeClass) {
        super(54555 + implement.getID() * 33 + changeClass.ID, implement.toString()); // changeClass тоже надо чтобы propertyView те же были

/*        // добавляем элементы для которых меняем на форму
        interfaceGroup = new GroupObjectNavigator(IDShift(1));
        interfaceGroup.singleViewType = true; interfaceGroup.initClassView = false;
        interfaceObjects = new HashMap<ClassPropertyInterface, ObjectNavigator>();
        for(ClassPropertyInterface propertyInterface : change.interfaces) {
            ObjectNavigator interfaceObject = new ObjectNavigator(propertyInterface.ID,propertyInterface.interfaceClass,propertyInterface.toString());
            interfaceObject.show = false;
            interfaceObjects.put(propertyInterface,interfaceObject);
            interfaceGroup.add(interfaceObject);
        }
        addGroup(interfaceGroup);*/

        // сам объект который ищем вешаем на форму
        valueGroup = new GroupObjectNavigator(IDShift(1));
        valueObject = new ObjectNavigator(IDShift(1), changeClass, "Код");
        valueGroup.add(valueObject);
        addGroup(valueGroup);

        for(MaxChangeProperty<?, P> constrainedProperty : BL.getChangeConstrainedProperties(implement.property)) // добавляем все констрейнты
            addFixedFilter(new NotFilterNavigator(new NotNullFilterNavigator<MaxChangeProperty.Interface<P>>(
                    constrainedProperty.getPropertyNavigator(implement.mapping, valueObject))));

        addControlView(BL.controls, BL.baseGroup, true, valueObject);
        addControlView(BL.controls, BL.aggrGroup, true, valueObject);
    }

/*    public <T extends BusinessLogics<T>> void seekObjects(RemoteForm<T> remoteForm, Mapper mapper, Object dataValue, Map<ClassPropertyInterface, Object> interfaceValues) {
//        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(interfaceGroup),BaseUtils.crossJoin(BaseUtils.join(interfaceObjects,mapper.objectMapper),interfaceValues));
        remoteForm.userGroupSeeks.put(mapper.groupMapper.get(valueGroup),Collections.singletonMap(mapper.objectMapper.get(valueObject),dataValue));
    }*/
}

