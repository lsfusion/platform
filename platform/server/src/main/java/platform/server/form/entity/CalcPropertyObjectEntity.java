package platform.server.form.entity;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.form.instance.CalcPropertyObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public class CalcPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, CalcProperty<P>> implements OrderEntity<CalcPropertyObjectInstance<P>> {

    public CalcPropertyObjectEntity() {
        //нужен для десериализации
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping) {
        super(property, (ImMap<P,PropertyObjectInterfaceEntity>) mapping, null, null);
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        super(property, (ImMap<P,PropertyObjectInterfaceEntity>) mapping, creationScript, creationPath);
    }

    @Override
    public CalcPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public CalcPropertyObjectEntity<P> getRemappedEntity(final ObjectEntity oldObject, final ObjectEntity newObject, final InstanceFactory instanceFactory) {
        ImMap<P, PropertyObjectInterfaceEntity> nmapping = mapping.mapValues(new GetValue<PropertyObjectInterfaceEntity, PropertyObjectInterfaceEntity>() {
            public PropertyObjectInterfaceEntity getMapValue(PropertyObjectInterfaceEntity value) {
                return value.getRemappedEntity(oldObject, newObject, instanceFactory);
            }});
        return new CalcPropertyObjectEntity<P>(property, nmapping, creationScript, creationPath);
    }

}
