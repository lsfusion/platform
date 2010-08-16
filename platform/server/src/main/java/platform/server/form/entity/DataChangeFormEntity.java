package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;

public class DataChangeFormEntity<T extends BusinessLogics<T>> extends ClassFormEntity<T> {

    public <P extends PropertyInterface> DataChangeFormEntity(T BL, CustomClass changeClass, PropertyValueImplement<P> implement) {
        super(BL, changeClass, 54555 + implement.getID() * 33 + changeClass.ID, implement.toString()); // changeClass тоже надо чтобы propertyView те же были

        for(MaxChangeProperty<?, P> constrainedProperty : BL.getChangeConstrainedProperties(implement.property)) // добавляем все констрейнты
            addFixedFilter(new NotFilterEntity(new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                    constrainedProperty.getPropertyObjectEntity(implement.mapping, object))));
    }
}

