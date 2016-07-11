package lsfusion.server.form.entity;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.instance.ActionPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.Instantiable;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class ActionPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, ActionProperty<P>> implements Instantiable<ActionPropertyObjectInstance<P>> {

    public ActionPropertyObjectEntity() {
        //нужен для десериализации
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping) {
        this(property, mapping, null, null);
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> mapping, String creationScript, String creationPath) {
        super(property, (ImMap<P,PropertyObjectInterfaceEntity>) mapping, creationScript, creationPath);
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
                if (object.baseClass instanceof CustomClass && simpleAdd.isChild((CustomClass) object.baseClass) && object.groupTo.getObjects().size()==1) {
                    return new Pair<ObjectEntity, Boolean>(object, true);
                }
        }

        P simpleDelete = property.getSimpleDelete();
        PropertyObjectInterfaceEntity object;
        if(simpleDelete!=null && (object = mapping.get(simpleDelete)) instanceof ObjectEntity && ((ObjectEntity)object).groupTo.getObjects().size()==1) {
            return new Pair<ObjectEntity, Boolean>((ObjectEntity)object, false);
        }

        return null;
    }
}
