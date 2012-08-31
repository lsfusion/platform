package platform.server.form.entity;

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

    public CalcPropertyObjectEntity(CalcProperty<P> property, Map<P, ? extends PropertyObjectInterfaceEntity> mapping) {
        super(property, (Map<P,PropertyObjectInterfaceEntity>) mapping, null, null);
    }

    public CalcPropertyObjectEntity(CalcProperty<P> property, Map<P, ? extends PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        super(property, (Map<P,PropertyObjectInterfaceEntity>) mapping, creationScript, creationPath);
    }

    @Override
    public CalcPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public CalcPropertyObjectEntity<P> getRemappedEntity(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        Map<P, PropertyObjectInterfaceEntity> nmapping = new HashMap<P, PropertyObjectInterfaceEntity>();    
        for (P iFace : property.interfaces) {
            nmapping.put(iFace, mapping.get(iFace).getRemappedEntity(oldObject, newObject, instanceFactory));
        }
        return new CalcPropertyObjectEntity<P>(property, nmapping, creationScript, creationPath);
    }

}
