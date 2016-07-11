package lsfusion.server.form.entity;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

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
        return new CalcPropertyObjectEntity<>(property, nmapping, creationScript, creationPath);
    }

}
