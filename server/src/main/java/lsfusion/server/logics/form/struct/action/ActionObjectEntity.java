package lsfusion.server.logics.form.struct.action;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.instance.property.ActionObjectInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.interactive.action.async.*;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionObjectEntity<P extends PropertyInterface> extends ActionOrPropertyObjectEntity<P, Action<P>> implements Instantiable<ActionObjectInstance<P>>, ActionObjectSelector {

    public ActionObjectEntity() {
        //нужен для десериализации
    }

    public ActionObjectEntity(LA<P> property) {
        this(property.action, MapFact.EMPTYREV());
    }
    public ActionObjectEntity(Action<P> property, ImRevMap<P, ObjectEntity> mapping) {
        this(property, mapping, null, null, null);
    }

    public ActionObjectEntity(Action<P> property, ImRevMap<P, ObjectEntity> mapping, String creationScript, String creationPath, String path) {
        super(property, mapping, creationScript, creationPath, path);
    }

    public ActionObjectInstance<P> getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ActionObjectEntity<?> getGroupChange(GroupObjectEntity entity, PropertyObjectEntity<?> readOnly) {
        if(entity == null || !entity.viewType.isList())
            return null;
        
        return this.property.getGroupChange(entity, mapping, readOnly);
    }

    public Property.MapSelect<?> getSelectProperty(boolean forceSelect, PropertyObjectEntity<?> drawProperty) {
        return property.getSelectProperty(forceSelect, mapping, drawProperty);
    }

    public AsyncEventExec getAsyncEventExec(FormInstanceContext context, ActionOrProperty securityProperty, PropertyDrawEntity drawProperty, GroupObjectEntity toDraw, boolean optimistic) {
        AsyncMapEventExec<P> asyncExec = property.getAsyncEventExec(optimistic);
        if(asyncExec != null)
            return asyncExec.map(mapping, context, securityProperty, drawProperty, toDraw);
        return null;
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

    private static PropertyObjectEntity TRUE;
    public static <X extends PropertyInterface> PropertyObjectEntity<X> TRUE() {
        if(TRUE == null) {
            TRUE = PropertyFact.createTrue().mapEntityObjects(MapFact.EMPTYREV());
        }
        return TRUE;
    }

    @Override
    public ActionObjectEntity<P> getAction(FormInstanceContext context) {
        return this;
    }
}
