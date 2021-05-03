package lsfusion.server.logics.form.struct.action;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.form.struct.property.async.*;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
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

    public ActionObjectEntity<?> getGroupChange(GroupObjectEntity entity) {
        if(entity == null || !entity.viewType.isList())
            return null;
        
        // [FILTER group]
        return this.property.getGroupChange(entity, mapping);
    }

    @IdentityLazy
    public AsyncAddRemove getAddRemove(FormEntity form) {
        CustomClass simpleAdd = property.getSimpleAdd();
        if(simpleAdd!=null) {
            for(ObjectEntity object : form.getObjects())
                if (object.baseClass instanceof CustomClass && simpleAdd.isChild((CustomClass) object.baseClass) && object.isSimpleList()) {
                    return new AsyncAddRemove(object, true);
                }
        }

        P simpleDelete = property.getSimpleDelete();
        PropertyObjectInterfaceEntity object;
        if(simpleDelete!=null && (object = mapping.get(simpleDelete)) instanceof ObjectEntity && ((ObjectEntity)object).isSimpleList()) {
            return new AsyncAddRemove((ObjectEntity) object, false);
        }

        return null;
    }

    @IdentityLazy
    public AsyncChange getChange(boolean optimistic) {
        Type changeType = property.getSimpleRequestInputType(optimistic);
        if(changeType!=null) {
            return new AsyncChange(changeType);
        }
        return null;
    }

    @IdentityLazy
    public AsyncExec getAsyncExec() {
        return property.getAsyncExec();
    }

    public AsyncEventExec getAsyncEventExec(FormEntity form, boolean optimistic) {
        AsyncEventExec asyncEventExec = getAddRemove(form);
        if (asyncEventExec == null) {
            asyncEventExec = getChange(optimistic);
        }
        if (asyncEventExec == null) {
            asyncEventExec = getAsyncExec();
        }
        return asyncEventExec;
    }

//    @IdentityInstanceLazy
//    private <X extends PropertyInterface> PropertyObjectEntity<?> getFullDrawProperty(PropertyObjectEntity<X> readOnly) {
//        ImSet<ObjectEntity> allObjects = mapping.valuesSet().merge(readOnly.mapping.valuesSet());
//        ImRevMap<ObjectEntity, PropertyInterface> objectInterfaces = allObjects.mapRevValues((Supplier<PropertyInterface>) PropertyInterface::new);
//
//        PropertyMapImplement<?, PropertyInterface> map = property.getWhereProperty().map(mapping.join(objectInterfaces));
//        readOnly.property.getIdentityImplement(readOnly.mapping.join(objectInterfaces))
//        return PropertyFact.createAnd(objectInterfaces.valuesSet(), map, )
//        readOnly.map
//    }
//

    @IdentityInstanceLazy
    public <X extends PropertyInterface> PropertyObjectEntity<?> getDrawProperty(PropertyObjectEntity<X> readOnly) {
        //        return PropertyFact.createTrue().mapObjects(MapFact.<PropertyInterface, PropertyObjectInterfaceInstance>EMPTY());
        if(readOnly == null) // optimization
            return PropertyFact.createTrue().mapEntityObjects(MapFact.EMPTYREV());
        return PropertyFact.createNot(readOnly.property.getImplement()).mapEntityObjects(readOnly.mapping);
    }
}
