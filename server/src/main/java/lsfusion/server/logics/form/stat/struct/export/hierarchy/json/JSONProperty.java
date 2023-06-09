package lsfusion.server.logics.form.stat.struct.export.hierarchy.json;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.file.JSONClass;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.stat.AbstractFormDataInterface;
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
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
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

    public JSONProperty(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls,
                            ImOrderSet<PropertyInterface> orderContextInterfaces, ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters) {
        super(caption, FormAction.getValueClasses(form, objectsToSet, orderContextInterfaces.size(), new ValueClass[0]));

        this.form = form;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        ImOrderSet<ClassPropertyInterface> objectInterfaces = orderInterfaces.subOrder(0, objectsToSet.size());

        mapObjects = objectInterfaces.mapOrderRevKeys(objectsToSet::get);
        this.notNullInterfaces = objectInterfaces.mapOrderValues(nulls::get).filterFnValues(element -> !element).keys();

        ImRevMap<PropertyInterface, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(objectsToSet.size(), objectsToSet.size() + orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        this.contextFilters = contextFilters.mapSetValues(filter -> filter.map(mapContextInterfaces));
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(JSONClass.instance);
    }

    @Override
    protected PropertyMapImplement<?, ClassPropertyInterface> createProperty() {
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> staticForm = this.form.getForm(getBaseLM());
        ImRevMap<ObjectEntity, ClassPropertyInterface> mappedObjects = staticForm.second.rightJoin(this.mapObjects);

        ImSet<GroupObjectEntity> valueGroups = AbstractFormDataInterface.getValueGroupObjects(mappedObjects.keys());

        StaticDataGenerator.Hierarchy staticHierarchy = staticForm.first.getStaticHierarchy(false, valueGroups, null);

        ParseNode parseNode = staticHierarchy.getIntegrationHierarchy();

        FormPropertyDataInterface<ClassPropertyInterface> formInterface = new FormPropertyDataInterface<>(staticForm.first, valueGroups, ContextFilterSelector.getEntities(contextFilters).mapSetValues(entity -> entity.mapObjects(staticForm.second.reverse())));

        return parseNode.getJSONProperty(formInterface, contextInterfaces.toRevMap(), mappedObjects);
    }

    private static ObjectValue getJSONObjectValue(ValueClass valueClass, Object jsonValue, DataSession session) throws SQLException, SQLHandledException {
        try {
            return session.getObjectValue(valueClass, valueClass.getType().parseJSON(jsonValue));
        } catch (ParseException e) {
            throw Throwables.propagate(e);
        }
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

        protected <T extends PropertyInterface> void change(JSONObject rootObject, ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
            String propertyID = rootObject.getString("property");

            PropertyDrawEntity<T> propertyDraw = (PropertyDrawEntity<T>) form.getPropertyDrawIntegration(propertyID);
            ImMap<ObjectEntity, ? extends ObjectValue> mapValues = mapObjects.join(context.getKeys());

            DataSession session = context.getSession();

            // INPUT f FIELDS o1 O1, o2 O2, v value DO
            //      changeProperty(x1, x2, o1, o2) <- v;

            JSONObject objects = rootObject.getJSONObject("object");

            PropertyObjectEntity<T> propertyObject = (PropertyObjectEntity<T>) propertyDraw.getValueProperty();
            Property<T> property = propertyObject.property;
            ImMap<T, ObjectValue> mapPropValues = propertyObject.mapping.<ObjectValue, SQLException, SQLHandledException>mapValuesEx(object -> {
                ObjectValue value = mapValues.get(object);
                if (value != null)
                    return value;

                return getJSONObjectValue(object.baseClass, objects.opt(object.getIntegrationSID()), session);
            });
            ImMap<T, DataObject> mapDataPropValues = DataObject.filterDataObjects(mapPropValues);
            if(mapDataPropValues.size() < mapPropValues.size())
                return;
            ObjectValue mapValue = getJSONObjectValue(property.getValueClass(ClassType.editValuePolicy), rootObject.opt("value"), session);

            property.change(mapDataPropValues, context.getEnv(), mapValue);
        }

        @Override
        public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
            return new AsyncMapInput<>(JSONClass.instance, null, null, false, null, null);
        }

        @Override
        protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
            InputResult pushedInput = context.getPushedInput(JSONClass.instance);
            if(pushedInput != null) {
                String charset = ExternalUtils.defaultXMLJSONCharset;
                try {
                    JSONObject rootObject = JSONReader.toJSONObject(JSONReader.readRootObject(ImportAction.readFile(pushedInput.value, charset), null, charset), true);
                    if(rootObject != null)
                        change(rootObject, context);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return null;
        }

        @Override
        public boolean hasFlow(ChangeFlowType type) {
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

