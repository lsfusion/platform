package lsfusion.server.logics.form.struct.property;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.instance.property.ActionPropertyObjectInstance;
import lsfusion.server.logics.form.interactive.InstanceFactory;
import lsfusion.server.logics.form.interactive.Instantiable;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class ActionPropertyObjectEntity<P extends PropertyInterface> extends PropertyObjectEntity<P, ActionProperty<P>> implements Instantiable<ActionPropertyObjectInstance<P>> {

    public ActionPropertyObjectEntity() {
        //нужен для десериализации
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, ImRevMap<P, ObjectEntity> mapping) {
        this(property, mapping, null, null);
    }

    public ActionPropertyObjectEntity(ActionProperty<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath) {
        super(property, mapping, creationScript, creationPath);
    }

    public ActionPropertyObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ActionPropertyObjectEntity<?> getGroupChange() {
        return property.getGroupChange().mapObjects(mapping);
    }

    public Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form) {
        CustomClass simpleAdd = property.getSimpleAdd();
        if(simpleAdd!=null) {
            for(ObjectEntity object : form.getObjects())
                if (object.baseClass instanceof CustomClass && simpleAdd.isChild((CustomClass) object.baseClass) && object.groupTo.getObjects().size()==1) {
                    return new Pair<>(object, true);
                }
        }

        P simpleDelete = property.getSimpleDelete();
        PropertyObjectInterfaceEntity object;
        if(simpleDelete!=null && (object = mapping.get(simpleDelete)) instanceof ObjectEntity && ((ObjectEntity)object).groupTo.getObjects().size()==1) {
            return new Pair<>((ObjectEntity) object, false);
        }

        return null;
    }
}
