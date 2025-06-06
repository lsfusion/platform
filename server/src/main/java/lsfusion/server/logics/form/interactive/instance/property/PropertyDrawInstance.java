package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
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
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapInput;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapValue;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputContextListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputPropertyValueList;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.AsyncDataConverter;
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
        public final InputValueList<?> list;
        public final AsyncDataConverter<P> converter;
        public final boolean newSession;
        public final AsyncMode asyncMode;

        public AsyncValueList(InputValueList<?> list, AsyncDataConverter<P> converter, boolean newSession, AsyncMode asyncMode) {
            this.list = list;
            this.converter = converter;
            this.newSession = newSession;
            this.asyncMode = asyncMode;
        }
    }

    public <P extends PropertyInterface, X extends PropertyInterface, Z extends PropertyInterface> AsyncValueList<?> getAsyncValueList(String actionSID, Result<String> value, FormInstanceContext context, FormInstance formInstance, ImMap<ObjectInstance, ? extends ObjectValue> keys) {

        Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter = (PropertyObjectInterfaceInstance po) -> {
            if(po instanceof ObjectInstance) {
                ObjectValue keyValue = keys.get((ObjectInstance) po);
                if (keyValue != null)
                    return keyValue;
            }
            return po.getObjectValue();
        };

        InputValueList<X> list;
        AsyncDataConverter<X> converter;
        boolean newSession;
        AsyncMode asyncMode;

        boolean needObjects = actionSID.equals(ServerResponse.OBJECTS);
        boolean strictValues = actionSID.equals(ServerResponse.STRICTVALUES);
        AsyncMode valuesMode = null;
        if(needObjects || strictValues || actionSID.equals(ServerResponse.VALUES)) { // filter or custom view
            int useFilters = needObjects ? 2 : Settings.get().getUseGroupFiltersInAsyncFilterCompletion();

            Result<ImRevMap<X, ObjectInstance>> rMapObjects = needObjects ? new Result<>() : null;

            list = getInputValueList(valuesGetter, rMapObjects, useFilters);
            if(list == null)
                return null;
            newSession = BaseUtils.nvl(this.entity.defaultChangeEventScope, needObjects ? PropertyDrawEntity.DEFAULT_OBJECTS_EVENTSCOPE : PropertyDrawEntity.DEFAULT_VALUES_EVENTSCOPE) == FormSessionScope.NEWSESSION;
            converter = needObjects ? values -> rMapObjects.result.crossJoin(values) : null;
            valuesMode = strictValues ? AsyncMode.STRICTVALUES : AsyncMode.VALUES;
        } else {
            ActionObjectEntity<P> eventAction = (ActionObjectEntity<P>) this.entity.getCheckedEventAction(actionSID, context);

            AsyncMapValue<P> asyncExec = (AsyncMapValue<P>) eventAction.property.getAsyncEventExec(this.entity.optimisticAsync);
            ImRevMap<P, PropertyObjectInterfaceInstance> outerMapping = BaseUtils.immutableCast(formInstance.instanceFactory.getInstanceMap(eventAction.mapping));

            Pair<InputContextListEntity<Z, P>, AsyncDataConverter<Z>> asyncValueList = asyncExec.getAsyncValueList(value);
            InputContextListEntity<Z, P> listEntity = asyncValueList.first;
            list = (InputValueList<X>) listEntity.map(outerMapping, valuesGetter);
            converter = (AsyncDataConverter<X>) asyncValueList.second; // here we assert that Z = X, because in the not null branch we use InputContextListEntity with no filter / orders constructor
            newSession = listEntity.isNewSession();
            if(asyncExec instanceof AsyncMapInput)
                valuesMode = AsyncMapInput.getAsyncMode(((AsyncMapInput<P>) asyncExec).strict);
            else
                assert converter != null; // so we don't need valuesMode
        }

        return new AsyncValueList<>(list, converter, newSession, converter != null ? AsyncMode.OBJECTS : valuesMode);
    }

    // filter / custom view
    private <X extends PropertyInterface> InputPropertyValueList<X> getInputValueList(Function<PropertyObjectInterfaceInstance, ObjectValue> valuesGetter, Result<ImRevMap<X, ObjectInstance>> rMapObjects, int useFilters) {
        // actually that all X can be different
        PropertyObjectInstance<X> valueProperty = (PropertyObjectInstance<X>) getValueProperty();

        InputPropertyValueList<X> list;
        if(useFilters > 0)
            valueProperty = FilterInstance.ifCached(valueProperty, toDraw.getFilters(GroupObjectInstance.NOVIEWUSERFILTER(this), false));

        list = (InputPropertyValueList<X>) valueProperty.getInputValueList(toDraw, rMapObjects, valuesGetter, useFilters == 2);
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

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyShowIf;
    public final PropertyObjectInstance<?> propertyReadOnly;
    public final PropertyObjectInstance<?> propertyFooter;
    public final PropertyObjectInstance<?> propertyGridElementClass;
    public final PropertyObjectInstance<?> propertyValueElementClass;
    public final PropertyObjectInstance<?> propertyCaptionElementClass;
    public final PropertyObjectInstance<?> propertyFont;
    public final PropertyObjectInstance<?> propertyBackground;
    public final PropertyObjectInstance<?> propertyForeground;
    public final PropertyObjectInstance<?> propertyImage;
    public final PropertyObjectInstance<?> propertyComment;
    public final PropertyObjectInstance<?> propertyCommentElementClass;
    public final PropertyObjectInstance<?> propertyPlaceholder;
    public final PropertyObjectInstance<?> propertyPattern;
    public final PropertyObjectInstance<?> propertyRegexp;
    public final PropertyObjectInstance<?> propertyRegexpMessage;
    public final PropertyObjectInstance<?> propertyTooltip;
    public final PropertyObjectInstance<?> propertyValueTooltip;
    public final PropertyObjectInstance<?> propertyCustomOptions;
    public final PropertyObjectInstance<?> propertyChangeKey;
    public final PropertyObjectInstance<?> propertyChangeMouse;
    public final ImList<PropertyObjectInstance<?>> propertiesAggrLast;

    public ExtraReaderInstance captionReader;
    public ShowIfReaderInstance showIfReader;
    public ExtraReaderInstance footerReader;
    public ExtraReaderInstance readOnlyReader;
    public ExtraReaderInstance gridElementClassReader;
    public ExtraReaderInstance valueElementClassReader;
    public ExtraReaderInstance captionElementClassReader;
    public ExtraReaderInstance fontReader;
    public ExtraReaderInstance backgroundReader;
    public ExtraReaderInstance foregroundReader;
    public ExtraReaderInstance imageReader;
    public ExtraReaderInstance commentReader;
    public ExtraReaderInstance commentElementClassReader;
    public ExtraReaderInstance placeholderReader;
    public ExtraReaderInstance patternReader;
    public ExtraReaderInstance regexpReader;
    public ExtraReaderInstance regexpMessageReader;
    public ExtraReaderInstance tooltipReader;
    public ExtraReaderInstance valueTooltipReader;
    public ExtraReaderInstance propertyCustomOptionsReader;
    public ExtraReaderInstance changeKeyReader;
    public ExtraReaderInstance changeMouseReader;
    public final ImOrderSet<LastReaderInstance> aggrLastReaders;

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                ActionOrPropertyObjectInstance<?, ?> actionOrProperty,
                                PropertyObjectInstance<?> cellProperty,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                ImMap<PropertyDrawExtraType, PropertyObjectInstance<?>> propertyExtras,
                                ImList<PropertyObjectInstance<?>> propertiesAggrLast) {
        super(entity);
        this.actionOrProperty = actionOrProperty;
        this.cellProperty = cellProperty;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;

        propertyCaption = propertyExtras.get(PropertyDrawExtraType.CAPTION);
        propertyShowIf = propertyExtras.get(PropertyDrawExtraType.SHOWIF);
        propertyReadOnly = propertyExtras.get(PropertyDrawExtraType.READONLYIF);
        propertyFooter = propertyExtras.get(PropertyDrawExtraType.FOOTER);
        propertyGridElementClass = isList() ? propertyExtras.get(PropertyDrawExtraType.GRIDELEMENTCLASS) : null;
        propertyValueElementClass = propertyExtras.get(PropertyDrawExtraType.VALUEELEMENTCLASS);
        propertyCaptionElementClass = propertyExtras.get(PropertyDrawExtraType.CAPTIONELEMENTCLASS);
        propertyFont = propertyExtras.get(PropertyDrawExtraType.FONT);
        propertyBackground = propertyExtras.get(PropertyDrawExtraType.BACKGROUND);
        propertyForeground = propertyExtras.get(PropertyDrawExtraType.FOREGROUND);
        propertyImage = propertyExtras.get(PropertyDrawExtraType.IMAGE);
        propertyComment = propertyExtras.get(PropertyDrawExtraType.COMMENT);
        propertyCommentElementClass = propertyExtras.get(PropertyDrawExtraType.COMMENTELEMENTCLASS);
        propertyPlaceholder = propertyExtras.get(PropertyDrawExtraType.PLACEHOLDER);
        propertyPattern = propertyExtras.get(PropertyDrawExtraType.PATTERN);
        propertyRegexp = propertyExtras.get(PropertyDrawExtraType.REGEXP);
        propertyRegexpMessage = propertyExtras.get(PropertyDrawExtraType.REGEXPMESSAGE);
        propertyTooltip = propertyExtras.get(PropertyDrawExtraType.TOOLTIP);
        propertyValueTooltip = propertyExtras.get(PropertyDrawExtraType.VALUETOOLTIP);
        propertyCustomOptions = propertyExtras.get(PropertyDrawExtraType.PROPERTY_CUSTOM_OPTIONS);
        propertyChangeKey = propertyExtras.get(PropertyDrawExtraType.CHANGEKEY);
        propertyChangeMouse = propertyExtras.get(PropertyDrawExtraType.CHANGEMOUSE);
        this.propertiesAggrLast = propertiesAggrLast;

        captionReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTION, propertyCaption);
        showIfReader = new ShowIfReaderInstance(PropertyDrawExtraType.SHOWIF, propertyShowIf);
        footerReader = new ExtraReaderInstance(PropertyDrawExtraType.FOOTER, propertyFooter);
        readOnlyReader = new ExtraReaderInstance(PropertyDrawExtraType.READONLYIF, propertyReadOnly);
        gridElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.GRIDELEMENTCLASS, propertyGridElementClass);
        valueElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.VALUEELEMENTCLASS, propertyValueElementClass);
        captionElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTIONELEMENTCLASS, propertyCaptionElementClass);
        fontReader = new ExtraReaderInstance(PropertyDrawExtraType.FONT, propertyFont);
        backgroundReader = new ExtraReaderInstance(PropertyDrawExtraType.BACKGROUND, propertyBackground);
        foregroundReader = new ExtraReaderInstance(PropertyDrawExtraType.FOREGROUND, propertyForeground);
        imageReader = new ExtraReaderInstance(PropertyDrawExtraType.IMAGE, propertyImage);
        commentReader = new ExtraReaderInstance(PropertyDrawExtraType.COMMENT, propertyComment);
        commentElementClassReader = new ExtraReaderInstance(PropertyDrawExtraType.COMMENTELEMENTCLASS, propertyCommentElementClass);
        placeholderReader = new ExtraReaderInstance(PropertyDrawExtraType.PLACEHOLDER, propertyPlaceholder);
        patternReader = new ExtraReaderInstance(PropertyDrawExtraType.PATTERN, propertyPattern);
        regexpReader = new ExtraReaderInstance(PropertyDrawExtraType.REGEXP, propertyRegexp);
        regexpMessageReader = new ExtraReaderInstance(PropertyDrawExtraType.REGEXPMESSAGE, propertyRegexpMessage);
        tooltipReader = new ExtraReaderInstance(PropertyDrawExtraType.TOOLTIP, propertyTooltip);
        valueTooltipReader = new ExtraReaderInstance(PropertyDrawExtraType.VALUETOOLTIP, propertyValueTooltip);
        propertyCustomOptionsReader = new ExtraReaderInstance(PropertyDrawExtraType.PROPERTY_CUSTOM_OPTIONS, propertyCustomOptions);
        changeKeyReader = new ExtraReaderInstance(PropertyDrawExtraType.CHANGEKEY, propertyChangeKey);
        changeMouseReader = new ExtraReaderInstance(PropertyDrawExtraType.CHANGEMOUSE, propertyChangeMouse);
        aggrLastReaders = SetFact.toOrderExclSet(propertiesAggrLast.size(), LastReaderInstance::new);
    }

    public PropertyObjectInstance getReaderProperty() {
        if(toDraw != null && toDraw.viewType.isList() && toDraw.listViewType.isValue())
            return getValueProperty();

        return getCellProperty();
    }

    // CELL (GRID / PANEL)
    @Override
    public PropertyObjectInstance getCellProperty() {
        return cellProperty;
    }

    // REST
    public PropertyObjectInstance<?> getValueProperty() {
        if(actionOrProperty instanceof PropertyObjectInstance)
            return (PropertyObjectInstance<?>) actionOrProperty;

        return cellProperty;
    }

    // PIVOT
    @Override
    public PropertyObjectInstance getGroupProperty() {
        return getValueProperty();
    }

    // SUM / GROUP DESKTOP
    public PropertyObjectInstance<?> getSumProperty() {
        return getValueProperty();
    }

    public PropertyObjectInstance<?> getFilterProperty() {
        return getValueProperty();
    }

    public PropertyObjectInstance<?> getOrderProperty() {
        return getValueProperty();
    }

    private final PropertyObjectInstance<?> cellProperty;

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
