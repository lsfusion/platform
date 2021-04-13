package lsfusion.server.logics.form.struct.action;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.input.SimpleDataInput;
import lsfusion.server.logics.form.interactive.action.input.SimpleRequestInput;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.open.interactive.SimpleDialogInput;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.async.*;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
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
        
        return this.property.getGroupChange(entity, mapping);
    }

    @IdentityStrongLazy // for security policy and maybe optimization
    public <T extends PropertyInterface> ActionObjectEntity<?> getChangeWYS(Property<T> eventProperty, boolean optimistic) {
        SimpleRequestInput<P> simpleInput = property.getSimpleRequestInput(optimistic);
        if (simpleInput instanceof SimpleDataInput)
            return this;

        if (simpleInput instanceof SimpleDialogInput) {
            SimpleDialogInput<P> simpleDialogInput = (SimpleDialogInput<P>) simpleInput;
            Property<?> viewProperty = eventProperty.getViewProperty(simpleDialogInput.customClass);
            if(viewProperty != null)
                return property.getDialogChangeWYS(simpleDialogInput.list.getView(viewProperty), simpleDialogInput.targetProp).mapObjects(mapping);
        }

        return null;
    }

    public AsyncEventExec getAsyncEventExec(FormEntity form, boolean optimistic) {
        CustomClass simpleAdd = property.getSimpleAdd();
        if(simpleAdd!=null) {
            for(ObjectEntity object : form.getObjects())
                if (object.baseClass instanceof CustomClass && simpleAdd.isChild((CustomClass) object.baseClass) && object.groupTo.getObjects().size()==1) {
                    return new AsyncAddRemove(object, true);
                }
        }

        P simpleDelete = property.getSimpleDelete();
        ObjectEntity object;
        if(simpleDelete!=null && (object = mapping.get(simpleDelete)) != null && object.groupTo.getObjects().size()==1)
            return new AsyncAddRemove(object, false);

        SimpleRequestInput<P> simpleInput = property.getSimpleRequestInput(optimistic);
        if(simpleInput instanceof SimpleDataInput) {
            SimpleDataInput<P> simpleDataInput = (SimpleDataInput<P>) simpleInput;
            return new AsyncChange(simpleDataInput.type, simpleDataInput.list != null, simpleDataInput.targetProp);
        }

        return property.getAsyncExec();
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
