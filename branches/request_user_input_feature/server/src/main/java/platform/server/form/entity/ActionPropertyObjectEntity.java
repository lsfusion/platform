package platform.server.form.entity;

import platform.server.form.instance.ActionPropertyObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Map;

public class ActionPropertyObjectEntity extends PropertyObjectEntity<ClassPropertyInterface, ActionProperty> implements Instantiable<ActionPropertyObjectInstance>, ServerCustomSerializable {

    public ActionPropertyObjectEntity(ActionProperty property, Map<ClassPropertyInterface, PropertyObjectInterfaceEntity> mapping, String creationScript) {
        super(property, mapping, creationScript);
    }

    public ActionPropertyObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }
}
