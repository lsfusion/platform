package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.FormChangeFlowType;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapValue;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputContextListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputContextPropertyListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputPropertyListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.AbstractFormDataInterface;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.hierarchy.*;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;

public class JSONProperty<O extends ObjectSelector> extends LazyProperty {

    public final FormSelector<O> form;
    public final ImRevMap<O, ClassPropertyInterface> mapObjects;

    private final ImSet<ClassPropertyInterface> notNullInterfaces;

    // CONTEXT
    protected final ImSet<ClassPropertyInterface> contextInterfaces;
    protected final ImSet<ContextFilterSelector<ClassPropertyInterface, O>> contextFilters;

    protected final SelectTop<ClassPropertyInterface> selectTopInterfaces;

    private boolean returnString;

    public JSONProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                            ImOrderSet<PropertyInterface> orderContextInterfaces,
                        ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters,
                        SelectTop<ValueClass> selectTop, same as in FormStatic action, adding to valueClasses
                        boolean returnString) {
        super(caption, FormAction.getValueClasses(form, objectsToSet, orderContextInterfaces.size(), new ValueClass[0]));

        this.form = form;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        ImOrderSet<ClassPropertyInterface> objectInterfaces = orderInterfaces.subOrder(0, objectsToSet.size());

        mapObjects = objectInterfaces.mapOrderRevKeys(objectsToSet::get);
        this.notNullInterfaces = objectInterfaces.mapOrderValues(nulls::get).filterFnValues(element -> !element).keys();

        ImRevMap<PropertyInterface, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(objectsToSet.size(), objectsToSet.size() + orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        this.contextFilters = contextFilters.mapSetValues(filter -> filter.map(mapContextInterfaces));

        selectTopInterfaces = same as in FormStatic action get from orderInterfaces

        this.returnString = returnString;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(returnString ? JSONTextClass.instance : JSONClass.instance);
    }

    @Override
    protected ImSet<ClassPropertyInterface> getNotNullInterfaces() {
        return notNullInterfaces;
    }

    @Override
    protected PropertyMapImplement<?, ClassPropertyInterface> createProperty() {
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> staticForm = this.form.getForm(getBaseLM());
        ImRevMap<ObjectEntity, ClassPropertyInterface> mappedObjects = staticForm.second.rightJoin(this.mapObjects);

        ImSet<GroupObjectEntity> valueGroups = AbstractFormDataInterface.getValueGroupObjects(mappedObjects.keys());

        StaticDataGenerator.Hierarchy staticHierarchy = staticForm.first.getStaticHierarchy(false, valueGroups, null);

        ParseNode parseNode = staticHierarchy.getIntegrationHierarchy();

        FormPropertyDataInterface<ClassPropertyInterface> formInterface = new FormPropertyDataInterface<>(staticForm.first, valueGroups, ContextFilterSelector.getEntities(contextFilters).mapSetValues(entity -> entity.mapObjects(staticForm.second.reverse())), selectTopInterfaces);

        return parseNode.getJSONProperty(formInterface, (contextInterfaces + selectTopInterfaces.getParams()).toRevMap(), mappedObjects, returnString);
    }

    private static ObjectValue fromJSON(ValueClass valueClass, Object jsonValue, DataSession session) throws SQLException, SQLHandledException {
        try {
            return session.getObjectValue(valueClass, valueClass.getType().parseJSON(jsonValue));
        } catch (ParseException e) {
            throw Throwables.propagate(e);
        }
    }
    private static Object toJSON(ValueClass valueClass, DataObject value) {
        return valueClass.getType().formatJSON(value.getValue());
    }

    // default change event action
    // which reads json - property, object and value, and changes the corresponding property
    // actually this action consists of :
    // INPUT JSON -> read property -> find property -> INPUT JSON with all keys -> CHANGE property
    // it could be implemented with the existing properties, but the implementation would be rather complicated so we just stick to this one
    private class ChangeAction extends SystemAction {

        private final FormEntity form;
        public final ImRevMap<ObjectEntity, PropertyInterface> mapObjects;

        public ChangeAction(LocalizedString caption, FormEntity form, ImOrderSet<ObjectEntity> objects) {
            super(caption, SetFact.toOrderExclSet(objects.size(), i -> new PropertyInterface()));

            this.form = form;
            mapObjects = objects.mapSet(getOrderInterfaces());
        }

        private class MapDraw<T extends PropertyInterface> {
            public final Property<T> property;
            public final ImRevMap<T, PropertyInterface> mapValues;
            public final ImRevMap<T, ObjectEntity> mapKeys;

            public MapDraw(Property<T> property, ImRevMap<T, PropertyInterface> mapValues, ImRevMap<T, ObjectEntity> mapKeys) {
                this.property = property;
                this.mapValues = mapValues;
                this.mapKeys = mapKeys;
            }
        }

        protected <T extends PropertyInterface> void change(JSONObject rootObject, ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
            String propertyID = rootObject.getString("property");
            DataSession session = context.getSession();

            // INPUT f FIELDS o1 O1, o2 O2, v value DO
            //      changeProperty(x1, x2, o1, o2) <- v;

            MapDraw<T> mapDraw = getPropertyDraw(propertyID);
            JSONObject objects = rootObject.getJSONObject("objects");
            ImMap<T, ? extends ObjectValue> mapPropValues = MapFact.addExcl(mapDraw.mapValues.join(context.getKeys()),
                    mapDraw.mapKeys.<ObjectValue, SQLException, SQLHandledException>mapValuesEx(object -> fromJSON(object.baseClass, objects.opt(object.getIntegrationSID()), session)));
            ImMap<T, DataObject> mapDataPropValues = DataObject.filterDataObjects(mapPropValues);
            if(mapDataPropValues.size() < mapPropValues.size())
                return;

            ObjectValue mapValue = fromJSON(mapDraw.property.getValueClass(ClassType.editValuePolicy), rootObject.opt("value"), session);

            mapDraw.property.change(mapDataPropValues, context.getEnv(), mapValue);
        }

        private <T extends PropertyInterface> MapDraw<T> getPropertyDraw(String propertyID) {
            PropertyObjectEntity<T> propertyObject = (PropertyObjectEntity<T>) form.getPropertyDrawIntegration(propertyID).getReaderProperty();

            Property<T> property = propertyObject.property;
            ImRevMap<T, PropertyInterface> mapObjectValues = propertyObject.mapping.innerJoin(mapObjects);
            ImRevMap<T, ObjectEntity> mapObjectKeys = propertyObject.mapping.removeRev(mapObjectValues.keys()); // Incl
            return new MapDraw<>(property, mapObjectValues, mapObjectKeys);
        }

        public class AsyncMapJSONChange<C extends PropertyInterface> extends AsyncMapValue<C> {

            public final ImRevMap<PropertyInterface, C> map;

            public AsyncMapJSONChange(ImRevMap<PropertyInterface, C> map) {
                super(returnString ? JSONTextClass.instance : JSONClass.instance);

                this.map = map;
            }

            @Override
            public AsyncMapEventExec<C> newSession() {
                return this;
            }

            @Override
            public <P extends PropertyInterface> AsyncMapEventExec<P> map(ImRevMap<C, P> mapping) {
                return new AsyncMapJSONChange<>(map.join(mapping));
            }

            @Override
            public <P extends PropertyInterface> AsyncMapEventExec<P> mapInner(ImRevMap<C, P> mapping) {
                ImRevMap<PropertyInterface, P> joinMapValues = PropertyMapImplement.mapInner(map, mapping);
                if(joinMapValues == null)
                    return null;

                return new AsyncMapJSONChange<>(joinMapValues);
            }

            @Override
            public <P extends PropertyInterface> AsyncMapEventExec<P> mapJoin(ImMap<C, PropertyInterfaceImplement<P>> mapping) {
                ImRevMap<PropertyInterface, P> joinMapValues = PropertyMapImplement.mapJoin(map, mapping);
                if(joinMapValues == null)
                    return null;

                return new AsyncMapJSONChange<>(joinMapValues);
            }

            @Override
            public AsyncMapEventExec<C> merge(AsyncMapEventExec<C> input) {
                if(!(input instanceof AsyncMapJSONChange))
                    return null;

                AsyncMapJSONChange<C> jsonInput = (AsyncMapJSONChange<C>) input;
                if(!BaseUtils.hashEquals(map, jsonInput.map))
                    return null;

                return this;
            }

            @Override
            public AsyncEventExec map(ImRevMap<C, ObjectEntity> mapObjects, ConnectionContext context, ActionOrProperty securityProperty, PropertyDrawEntity<?> drawProperty, GroupObjectEntity toDraw) {
                return new AsyncInput(type, null, null, null);
            }

            @Override
            public <X extends PropertyInterface> Pair<InputContextListEntity<X, C>, AsyncDataConverter<X>> getAsyncValueList(Result<String> value) {
                int separator = value.result.indexOf(":"); // should correspond CustomCellRenderer.getPropertyValues
                String propertyID = value.result.substring(0, separator);
                value.set(value.result.substring(separator + 1));

                MapDraw<X> mapDraw = getPropertyDraw(propertyID);

                return new Pair<>(new InputContextPropertyListEntity<>(new InputPropertyListEntity<>(mapDraw.property, mapDraw.mapValues.join(map))), values -> {
                    JSONObject objects = new JSONObject();
                    for(int i = 0, size = values.size(); i < size; i++) {
                        ObjectEntity object = mapDraw.mapKeys.get(values.getKey(i));
                        objects.putOpt(object.getIntegrationSID(), toJSON(object.baseClass, values.getValue(i)));
                    }
                    return objects.toString();
                });
            }
        }

        @Override
        public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
            return new AsyncMapJSONChange<>(interfaces.toRevMap());
        }

        @Override
        protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
            // in theory all changeAction should be split to input and change, and wrapped into Request, just like in Property.getJoinDefaultEventAction
            // but it will require too much refactoring, so we'll just use the hack with no drop (since this ChangeAction is also a sort of hack)
            InputResult pushedInput = context.getPushedInput(returnString ? JSONTextClass.instance : JSONClass.instance, false);
            context.dropRequestCanceled(); // need this because in group change push request there is a request canceled check
            if(pushedInput != null) {
                try {
                    // later maybe it makes sense to use simple new JSONObject() (since toString is used in getAsyncValues)
                    JSONObject rootObject = JSONReader.toJSONObject(InternalAction.readJSON(pushedInput.value), true);
                    if(rootObject != null)
                        change(rootObject, context);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return FlowResult.FINISH;
        }

        @Override
        public boolean hasFlow(ChangeFlowType type) {
            if(type instanceof FormChangeFlowType && !form.hasNoChange((FormChangeFlowType) type))
                return true;
            if(type == ChangeFlowType.ANYEFFECT)
                return true;
            return super.hasFlow(type);
        }
    }

    @Override
    @IdentityStrongLazy
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        if(eventActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;

        Pair<FormEntity, ImRevMap<ObjectEntity, O>> staticForm = this.form.getForm(getBaseLM());
        ImRevMap<ObjectEntity, ClassPropertyInterface> mappedObjects = staticForm.second.rightJoin(this.mapObjects);

        ChangeAction changeAction = new ChangeAction(LocalizedString.NONAME, staticForm.first, mappedObjects.keys().toOrderSet());
        return new ActionMapImplement<>(changeAction, changeAction.mapObjects.crossJoin(mappedObjects));
    }
}

