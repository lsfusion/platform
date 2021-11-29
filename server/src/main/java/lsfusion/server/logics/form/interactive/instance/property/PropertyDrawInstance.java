package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.BaseUtils;
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
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

import java.sql.SQLException;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity<P>> implements AggrReaderInstance {

    public ActionObjectInstance getEventAction(String actionId, FormInstance formInstance, SQLCallable<Boolean> checkReadOnly, SecurityPolicy securityPolicy) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> eventAction = entity.getEventAction(actionId, formInstance.entity, checkReadOnly, securityPolicy);
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

    public <P extends PropertyInterface, X extends PropertyInterface> AsyncValueList<?> getAsyncValueList(String actionSID, FormInstance formInstance, ImMap<ObjectInstance, ? extends ObjectValue> keys) {
        ActionOrPropertyObjectEntity<P, ?> mapEntity;
        InputListEntity<X, P> list;
        AsyncMode asyncMode;
        if(actionSID.equals(ServerResponse.FILTER)) {
            PropertyObjectEntity<P> drawProperty = (PropertyObjectEntity<P>) entity.getDrawProperty();
            list = (InputListEntity<X, P>) drawProperty.getFilterInputList(entity.getToDraw(formInstance.entity));
            if(list == null)
                return null;
            if(BaseUtils.nvl(entity.defaultChangeEventScope, PropertyDrawEntity.DEFAULT_FILTER_EVENTSCOPE) == FormSessionScope.NEWSESSION)
                list = list.newSession();
            mapEntity = drawProperty;
            asyncMode = AsyncMode.VALUES;
        } else if(actionSID.equals(ServerResponse.VALUES)) {
            PropertyObjectEntity<P> drawProperty = (PropertyObjectEntity<P>) entity.getDrawProperty();
            GroupObjectEntity groupObject = entity.getToDraw(formInstance.entity);
            // assert that X == P (used in (ImRevMap<X, ObjectInstance>) mapObjectInstances cast)
            list = (InputListEntity<X, P>) drawProperty.getValuesInputList(groupObject);
//            list = list.and(groupObject.getInputFilterEntity())
            if(BaseUtils.nvl(entity.defaultChangeEventScope, PropertyDrawEntity.DEFAULT_VALUES_EVENTSCOPE) == FormSessionScope.NEWSESSION)
                list = list.newSession();
            mapEntity = drawProperty;
            asyncMode = AsyncMode.OBJECTS;
        } else {
            ActionObjectEntity<P> eventAction = (ActionObjectEntity<P>) entity.getEventAction(actionSID, formInstance.entity);
            AsyncMapChange<P> asyncExec = (AsyncMapChange<P>) eventAction.property.getAsyncEventExec(entity.optimisticAsync);
            list = (InputListEntity<X, P>) asyncExec.list;
            asyncMode = asyncExec.inputList.strict ? AsyncMode.OBJECTVALUES : AsyncMode.VALUES;
            mapEntity = eventAction;
        }

        ImRevMap<P, ObjectInstance> mapObjectInstances = formInstance.instanceFactory.getInstanceMap(mapEntity);
        return new AsyncValueList<X>(list.map(mapObjectInstances.mapValues((ObjectInstance objectInstance) -> {
            ObjectValue keyValue = keys.get(objectInstance);
            if (keyValue != null)
                return keyValue;
            return objectInstance.getObjectValue();
        })), asyncMode == AsyncMode.OBJECTS ? ((ImRevMap<X, ObjectInstance>) mapObjectInstances).filterInclRev(list.getInterfaces()) : null, list.newSession, asyncMode);
    }

    private ActionOrPropertyObjectInstance<?, ?> propertyObject;
    
    public ActionOrPropertyObjectInstance<?, ?> getValueProperty() {
        return propertyObject;
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        return getValueProperty().isInInterface(classGroups, any);
    }

    public OrderInstance getOrder() {
        return (PropertyObjectInstance) getValueProperty();
    }
    
    public boolean isProperty() {
        return getValueProperty() instanceof PropertyObjectInstance;
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

    public Type getType() {
        return entity.getType();
    }

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyShowIf;
    public final PropertyObjectInstance<?> propertyReadOnly;
    public final PropertyObjectInstance<?> propertyFooter;
    public final PropertyObjectInstance<?> propertyBackground;
    public final PropertyObjectInstance<?> propertyForeground;
    public final PropertyObjectInstance<?> propertyImage;
    public final ImList<PropertyObjectInstance<?>> propertiesAggrLast;

    public ExtraReaderInstance captionReader;
    public ShowIfReaderInstance showIfReader;
    public ExtraReaderInstance footerReader;
    public ExtraReaderInstance readOnlyReader;
    public ExtraReaderInstance backgroundReader;
    public ExtraReaderInstance foregroundReader;
    public ExtraReaderInstance imageReader;
    public final ImOrderSet<LastReaderInstance> aggrLastReaders;

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                ActionOrPropertyObjectInstance<?, ?> propertyObject,
                                PropertyObjectInstance<?> drawProperty,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                ImMap<PropertyDrawExtraType, PropertyObjectInstance<?>> propertyExtras,
                                ImList<PropertyObjectInstance<?>> propertiesAggrLast) {
        super(entity);
        this.propertyObject = propertyObject;
        this.drawProperty = drawProperty;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;

        propertyCaption = propertyExtras.get(PropertyDrawExtraType.CAPTION);
        propertyShowIf = propertyExtras.get(PropertyDrawExtraType.SHOWIF);
        propertyReadOnly = propertyExtras.get(PropertyDrawExtraType.READONLYIF);
        propertyFooter = propertyExtras.get(PropertyDrawExtraType.FOOTER);
        propertyBackground = propertyExtras.get(PropertyDrawExtraType.BACKGROUND);
        propertyForeground = propertyExtras.get(PropertyDrawExtraType.FOREGROUND);
        propertyImage = propertyExtras.get(PropertyDrawExtraType.IMAGE);
        this.propertiesAggrLast = propertiesAggrLast;

        captionReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTION, propertyCaption);
        showIfReader = new ShowIfReaderInstance(PropertyDrawExtraType.SHOWIF, propertyShowIf);
        footerReader = new ExtraReaderInstance(PropertyDrawExtraType.FOOTER, propertyFooter);
        readOnlyReader = new ExtraReaderInstance(PropertyDrawExtraType.READONLYIF, propertyReadOnly);
        backgroundReader = new ExtraReaderInstance(PropertyDrawExtraType.BACKGROUND, propertyBackground);
        foregroundReader = new ExtraReaderInstance(PropertyDrawExtraType.FOREGROUND, propertyForeground);
        imageReader = new ExtraReaderInstance(PropertyDrawExtraType.IMAGE, propertyImage);
        aggrLastReaders = SetFact.toOrderExclSet(propertiesAggrLast.size(), LastReaderInstance::new);
    }

    public PropertyObjectInstance getPropertyObjectInstance() {
        return getDrawInstance();
    }

    public PropertyObjectInstance<?> getDrawInstance() {
        return drawProperty;
    }

    private final PropertyObjectInstance<?> drawProperty;

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public boolean isList() {
        return (toDraw != null ? toDraw.viewType : ClassViewType.PANEL).isList() && entity.viewType.isList();
    }

    public String toString() {
        return propertyObject.toString();
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

        public PropertyObjectInstance getPropertyObjectInstance() {
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
        public PropertyObjectInstance getPropertyObjectInstance() {
            return property;
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
    public PropertyDrawInstance getProperty() {
        return this;
    }

    public class LastReaderInstance implements AggrReaderInstance {
        public final int index;

        public LastReaderInstance(int index) {
            this.index = index;
        }

        @Override
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertiesAggrLast.get(index);
        }

        @Override
        public PropertyDrawInstance getProperty() {
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
