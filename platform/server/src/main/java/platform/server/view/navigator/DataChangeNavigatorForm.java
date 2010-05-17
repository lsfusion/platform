package platform.server.view.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.view.navigator.filter.NotFilterNavigator;
import platform.server.view.navigator.filter.NotNullFilterNavigator;
import platform.server.classes.CustomClass;

public class DataChangeNavigatorForm<T extends BusinessLogics<T>> extends ClassNavigatorForm<T> {

    public <P extends PropertyInterface> DataChangeNavigatorForm(T BL, CustomClass changeClass, PropertyValueImplement<P> implement) {
        super(BL, changeClass, 54555 + implement.getID() * 33 + changeClass.ID, implement.toString()); // changeClass тоже надо чтобы propertyView те же были

        for(MaxChangeProperty<?, P> constrainedProperty : BL.getChangeConstrainedProperties(implement.property)) // добавляем все констрейнты
            addFixedFilter(new NotFilterNavigator(new NotNullFilterNavigator<MaxChangeProperty.Interface<P>>(
                    constrainedProperty.getPropertyNavigator(implement.mapping, object))));
    }
}

