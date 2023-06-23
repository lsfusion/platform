package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.function.Function;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity<P>> implements AggrReaderInstance {

    public ActionObjectInstance getEventAction(String actionId, FormInstanceContext context, FormInstance formInstance, SQLCallable<Boolean> checkReadOnly) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> eventAction = entity.getCheckedEventAction(actionId, context, checkReadOnly);
        if(eventAction != null)
            return formInstance.instanceFactory.getInstance(eventAction);
        return null;
    }

    public static class AsyncValueList<P extends PropertyInterface> {
        public final InputValueList<P> list;
        public final ImRevMap<P, ObjectInstance> mapObjects;
        public final boolean newSession;
        public final AsyncMode asyncMode;

        public AsyncValueList(InputValueList<P> list, ImRevMap<P, ObjectInstance> mapObjects, boolean newSession, AsyncMode asyncMode) {
            this.list = list;
            this.mapObjects = mapObjects;
            this.newSession = newSession;
            this.asyncMode = asyncMode;
        }
    }

    public <P extends PropertyInterface, X extends PropertyInterface> AsyncValueList<?> getAsyncValueList(String actionSID, FormInstanceContext context, FormInstance formInstance, ImMap<ObjectInstance, ? extends ObjectValue> keys) {

        Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter = (PropertyObjectInterfaceInstance po) -> {
            if(po instanceof ObjectInstance) {
                ObjectValue keyValue = keys.get((ObjectInstance) po);
                if (keyValue != null)
                    return keyValue;
            }
            return po.getObjectValue();
        };

        InputValueList<X> list;
        ImRevMap<X, ObjectInstance> mapObjects;
        boolean newSession;
        AsyncMode asyncMode;

        boolean needObjects = actionSID.equals(ServerResponse.OBJECTS);
        boolean strictValues = actionSID.equals(ServerResponse.STRICTVALUES);
        if(needObjects || strictValues || actionSID.equals(ServerResponse.VALUES)) { // filter or custom view
            int useFilters = needObjects ? 2 : Settings.get().getUseGroupFiltersInAsyncFilterCompletion();

            Result<ImRevMap<X, ObjectInstance>> rMapObjects = needObjects ? new Result<>() : null;

            list = getInputValueList(valuesGetter, rMapObjects, useFilters);
            if(list == null)
                return null;
            newSession = BaseUtils.nvl(this.entity.defaultChangeEventScope, needObjects ? PropertyDrawEntity.DEFAULT_OBJECTS_EVENTSCOPE : PropertyDrawEntity.DEFAULT_VALUES_EVENTSCOPE) == FormSessionScope.NEWSESSION;
            mapObjects = needObjects ? rMapObjects.result : null;
            asyncMode = needObjects ? AsyncMode.OBJECTS : (strictValues ? AsyncMode.STRICTVALUES : AsyncMode.VALUES);
        } else {
            ActionObjectEntity<P> eventAction = (ActionObjectEntity<P>) this.entity.getEventAction(actionSID, context);
            AsyncMapInput<P> asyncExec = (AsyncMapInput<P>) eventAction.property.getAsyncEventExec(this.entity.optimisticAsync);
            InputListEntity<X, P> listEntity = (InputListEntity<X, P>) asyncExec.list;
            list = listEntity.map(formInstance.instanceFactory.getInstanceMap(eventAction.mapping).mapValues(BaseUtils.<Function<ObjectInstance, ObjectValue>>immutableCast(valuesGetter)));
            mapObjects = null;
            newSession = listEntity.newSession;
            asyncMode = asyncExec.inputList.strict ? AsyncMode.OBJECTVALUES : AsyncMode.VALUES;
        }

        return new AsyncValueList<>(list, mapObjects, newSession, asyncMode);
    }

    // filter / custom view
    private <X extends PropertyInterface> InputValueList<X> getInputValueList(Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter, Result<ImRevMap<X, ObjectInstance>> rMapObjects, int useFilters) {
        // actually that all X can be different
        PropertyObjectInstance<X> valueProperty = (PropertyObjectInstance<X>) getProperty();

        InputValueList<X> list;
        if(useFilters > 0)
            valueProperty = FilterInstance.ifCached(valueProperty, toDraw.getFilters(GroupObjectInstance.NOVIEWUSERFILTER(this), false));

        list = (InputValueList<X>) valueProperty.getInputValueList(toDraw, rMapObjects, valuesGetter, useFilters == 2);
        if(list == null && useFilters <= 0) // trying to use filters
            return getInputValueList(valuesGetter, rMapObjects, 1);

        return list;
    }

    private ActionOrPropertyObjectInstance<?, ?> actionOrProperty;

    public ActionOrPropertyObjectInstance<?, ?> getActionOrProperty() {
        return actionOrProperty;
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        return getActionOrProperty().isInInterface(classGroups, any);
    }

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null, кроме когда без параметров в FormInstance проставляется

    private final ImOrderSet<GroupObjectInstance> columnGroupObjects;
    public ImSet<GroupObjectInstance> getColumnGroupObjects() {
        return columnGroupObjects.getSet();
    }
    public ImOrderSet<GroupObjectInstance> getOrderColumnGroupObjects() {
        return columnGroupObjects;
    }
    @IdentityLazy
    public ImSet<GroupObjectInstance> getColumnGroupObjectsInGrid() {
        return getColumnGroupObjects().filterFn(element -> element.viewType.isList());
    }
    @IdentityLazy
    public ImSet<GroupObjectInstance> getGroupObjectsInGrid() {
        ImSet<GroupObjectInstance> result = getColumnGroupObjectsInGrid();
        if(isList())
            result = result.addExcl(toDraw);
        return result;
    }

    public boolean isProperty(FormInstanceContext context) {
        return entity.isProperty(context);
    }

    public Type getType(FormInstanceContext context) {
        return entity.getType(context);
    }

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyShowIf;
    public final PropertyObjectInstance<?> propertyReadOnly;
    public final PropertyObjectInstance<?> propertyFooter;
    public final PropertyObjectInstance<?> propertyValueElementClass;
    public final PropertyObjectInstance<?> propertyCaptionElementClass;
    public final PropertyObjectInstance<?> propertyBackground;
    public final PropertyObjectInstance<?> propertyForeground;
    public final PropertyObjectInstance<?> propertyImage;
    public final ImList<PropertyObjectInstance<?>> propertiesAggrLast;

    public ExtraReaderInstance captionReader;
    public ShowIfReaderInstance showIfReader;
    public ExtraReaderInstance footerReader;
    public ExtraReaderInstance readOnlyReader;
    public ExtraReaderInstance valueElementClassReader;
    public ExtraReaderInstance captionElementClassReader;
    public ExtraReaderInstance backgroundReader;
    public ExtraReaderInstance foregroundReader;
    public ExtraReaderInstance imageReader;
    public final ImOrderSet<LastReaderInstance> aggrLastReaders;

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                ActionOrPropertyObjectInstance<?, ?> actionOrProperty,
                                PropertyObjectInstance<?> property,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                ImMap<PropertyDrawExtraType, PropertyObjectInstance<?>> propertyExtras,
                                ImList<PropertyObjectInstance<?>> propertiesAggrLast) {
        super(entity);
        this.actionOrProperty = actionOrProperty;
        this.property = property;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;

        propertyCaption = propertyExtras.get(PropertyDrawExtraType.CAPTION);
        propertyShowIf = propertyExtras.get(PropertyDrawExtraType.SHOWIF);
        propertyReadOnly = propertyExtras.get(PropertyDrawExtraType.READONLYIF);
        propertyFooter = propertyExtras.get(PropertyDrawExtraType.FOOTER);
        propertyValueElementClass = propertyExtras.get(PropertyDrawExtraType.VALUEELEMENTCLASS);
        propertyCaptionElementClass = propertyExtras.get(PropertyDrawExtraType.CAPTIONELEMENTCLASS);
        propertyBackground = propertyExtras.get(PropertyDrawExtraType.BACKGROUND);
        propertyForeground = propertyExtras.get(PropertyDrawExtraType.FOREGROUND);
        propertyImage = propertyExtras.get(PropertyDrawExtraType.IMAGE);
        this.propertiesAggrLast = propertiesAggrLast;

        captionReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTION, propertyCaption);
        showIfReader = new ShowIfReaderInstance(PropertyDrawExtraType.SHOWIF, propertyShowIf);
        footerReader = new ExtraReaderInstance(PropertyDrawExtraType.FOOTER, propertyFooter);
        readOnlyReader = new ExtraReaderInstance(PropertyDrawExtraType.READONLYIF, propertyReadOnly);
        valueElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.VALUEELEMENTCLASS, propertyValueElementClass);
        captionElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTIONELEMENTCLASS, propertyCaptionElementClass);
        backgroundReader = new ExtraReaderInstance(PropertyDrawExtraType.BACKGROUND, propertyBackground);
        foregroundReader = new ExtraReaderInstance(PropertyDrawExtraType.FOREGROUND, propertyForeground);
        imageReader = new ExtraReaderInstance(PropertyDrawExtraType.IMAGE, propertyImage);
        aggrLastReaders = SetFact.toOrderExclSet(propertiesAggrLast.size(), LastReaderInstance::new);
    }

    // PIVOT / GRID
    public PropertyObjectInstance getReaderProperty() {
        return getProperty();
    }

    public PropertyObjectInstance<?> getProperty() {
        return property;
    }

    // DEPRECATED
    public PropertyObjectInstance<?> getValueProperty() {
        return getProperty();
    }
    // SUM / GROUP / DEPRECATED
    public PropertyObjectInstance<?> getGroupProperty() {
        return getProperty();
    }

    public PropertyObjectInstance<?> getFilterProperty() {
        return getProperty();
    }

    public PropertyObjectInstance<?> getOrderProperty() {
        return getProperty();
    }

    private final PropertyObjectInstance<?> property;

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public boolean isList() {
        return (toDraw != null ? toDraw.viewType : ClassViewType.PANEL).isList() && entity.viewType.isList();
    }

    public String toString() {
        return actionOrProperty.toString();
    }

    public PropertyDrawEntity getEntity() {
        return entity;
    }

    public String getIntegrationSID() {
        return entity.getIntegrationSID();
    }

    @Override
    public Object getProfiledObject() {
        return entity;
    }

    // заглушка чтобы на сервере ничего не читать
    public class HiddenReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getReaderProperty() {
            return new PropertyObjectInstance<>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        public byte getTypeID() {
            return PropertyDrawInstance.this.getTypeID();
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return NullValueProperty.instance;
        }
    }

    public class ExtraReaderInstance implements PropertyReaderInstance {
        private final PropertyDrawExtraType type;
        private final PropertyObjectInstance property;

        public ExtraReaderInstance(PropertyDrawExtraType type, PropertyObjectInstance property) {
            this.type = type;
            this.property = property;
        }

        @Override
        public PropertyObjectInstance getReaderProperty() {
            return property;
        }

        public PropertyDrawExtraType getType() {
            return type;
        }

        @Override
        public byte getTypeID() {
            return type.getPropertyReadType();
        }

        @Override
        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return entity.getPropertyExtra(type);
        }

        public String toString() {
            return ThreadLocalContext.localize(type.getText()) + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        public PropertyDrawInstance<P> getPropertyDraw() {
            return PropertyDrawInstance.this;
        }
    }

    public class ShowIfReaderInstance extends ExtraReaderInstance {
        public ShowIfReaderInstance(PropertyDrawExtraType type, PropertyObjectInstance property) {
            super(type, property);
        }
    }

    @Override
    public PropertyDrawInstance getAggrProperty() {
        return this;
    }

    public class LastReaderInstance implements AggrReaderInstance {
        public final int index;

        public LastReaderInstance(int index) {
            this.index = index;
        }

        @Override
        public PropertyObjectInstance getReaderProperty() {
            return propertiesAggrLast.get(index);
        }

        @Override
        public PropertyDrawInstance getAggrProperty() {
            return PropertyDrawInstance.this;
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.LAST;
        }

        @Override
        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return entity.lastAggrColumns.get(index);
        }
    }
}
