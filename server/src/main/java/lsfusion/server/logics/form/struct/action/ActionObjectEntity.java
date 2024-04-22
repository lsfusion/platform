package lsfusion.server.logics.form.struct.action;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.input.InputContextSelector;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputOrderEntity;
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
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.function.IntFunction;
import java.util.function.Supplier;

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

    public ActionObjectEntity<?> getGroupChange(GroupObjectEntity entity) {
        if(entity == null || !entity.viewType.isList())
            return null;
        
        return this.property.getGroupChange(entity, mapping);
    }

    public <X extends PropertyInterface> PropertyObjectEntity.Select getSelectProperty(FormInstanceContext context, boolean forceSelect, Boolean forceFilterSelected, PropertyObjectEntity<X> drawProperty) { // false - filter selected,
        AsyncMapEventExec<P> asyncExec = property.getAsyncEventExec(true);
        if(asyncExec instanceof AsyncMapInput) {
            AsyncMapInput<P> asyncMapInput = (AsyncMapInput<P>) asyncExec;

            if(asyncMapInput.list != null && asyncMapInput.strict) {
                // setting oldValue
                PropertyInterfaceImplement<P> oldValue = asyncMapInput.oldValue;
                boolean drawnValue = false;
                if (oldValue == null) {
                    drawnValue = true;
                    ImSet<ObjectEntity> allObjects = mapping.valuesSet().merge(drawProperty.mapping.valuesSet());
                    if (allObjects.size() > mapping.size()) { // optimization, when we don't have extra objects, just use existing
                        ImRevMap<ObjectEntity, PropertyInterface> objectInterfaces = allObjects.mapRevValues((Supplier<PropertyInterface>) PropertyInterface::new);
                        return getSelectProperty(context, forceSelect, forceFilterSelected, objectInterfaces.reverse(), asyncMapInput.map(mapping.join(objectInterfaces)), drawProperty.getImplement(objectInterfaces), drawnValue);
                    } else
                        oldValue = drawProperty.property.getIdentityImplement(drawProperty.mapping.crossValuesRev(mapping));
                }

                return getSelectProperty(context, forceSelect, forceFilterSelected, mapping, asyncMapInput, oldValue, drawnValue);
            }
        }
        return null;
    }

    private static <X extends PropertyInterface, Z extends PropertyInterface, Y extends PropertyInterface> PropertyObjectEntity.Select getSelectProperty(FormInstanceContext context, boolean forceSelect, Boolean forceFilterSelected, ImRevMap<X, ObjectEntity> mapping, AsyncMapInput<X> input, PropertyInterfaceImplement<X> value, boolean drawnValue) { // false - filter selected,
        InputListEntity<Z, X> list = (InputListEntity<Z, X>) input.list;
        return getSelectProperty(context, forceFilterSelected, Property.getSelectProperty(forceSelect, mapping.keys(), input.list, list.getInputFilterEntity(), list.getInputOrderEntities(), value, drawnValue), mapping);
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
