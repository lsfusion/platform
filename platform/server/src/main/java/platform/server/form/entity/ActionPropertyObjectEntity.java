package platform.server.form.entity;

import platform.base.Pair;
import platform.server.classes.CustomClass;
import platform.server.form.instance.ActionPropertyObjectInstance;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.Instantiable;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerCustomSerializable;

import java.util.Map;

public class ActionPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, ActionProperty<P>> implements Instantiable<ActionPropertyObjectInstance<P>>, ServerCustomSerializable {

    public ActionPropertyObjectEntity() {
        //нужен для десериализации
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, Map<P, ? extends PropertyObjectInterfaceEntity> mapping) {
        this(property, mapping, null, null);
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, Map<P, ? extends PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        super(property, (Map<P,PropertyObjectInterfaceEntity>) mapping, creationScript, creationPath);
    }

    public ActionPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ActionPropertyObjectEntity<?> getGroupChange() {
        return property.getGroupChange().mapObjects(mapping);
    }

    public Pair<ObjectEntity, Boolean> getAddRemove(FormEntity<?> form) {
        CustomClass simpleAdd = property.getSimpleAdd();
        if(simpleAdd!=null) {
            for(ObjectEntity object : form.getObjects())
                if (object.baseClass instanceof CustomClass && simpleAdd.isChild((CustomClass) object.baseClass) && object.groupTo.objects.size()==1) {
                    return new Pair<ObjectEntity, Boolean>(object, true);
                }
        }

        P simpleDelete = property.getSimpleDelete();
        PropertyObjectInterfaceEntity object;
        if(simpleDelete!=null && (object = mapping.get(simpleDelete)) instanceof ObjectEntity && ((ObjectEntity)object).groupTo.objects.size()==1) {
            return new Pair<ObjectEntity, Boolean>((ObjectEntity)object, false);
        }

        return null;
    }
}
