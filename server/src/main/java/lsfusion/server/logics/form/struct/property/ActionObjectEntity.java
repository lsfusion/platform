package lsfusion.server.logics.form.struct.property;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.interactive.instance.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.Instantiable;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionObjectEntity<P extends PropertyInterface> extends ActionOrPropertyObjectEntity<P, Action<P>> implements Instantiable<ActionObjectInstance<P>> {

    public ActionObjectEntity() {
        //нужен для десериализации
    }

    public ActionObjectEntity(Action<P> property, ImRevMap<P, ObjectEntity> mapping) {
        this(property, mapping, null, null);
    }

    public ActionObjectEntity(Action<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath) {
        super(property, mapping, creationScript, creationPath);
    }

    public ActionObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ActionObjectEntity<?> getGroupChange() {
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
