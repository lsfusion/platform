package lsfusion.server.logics.form.struct.property;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.*;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFMap;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.action.input.InputPropertyListEntity;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;
    
    public final ActionOrPropertyObjectEntity<P, ?> actionOrProperty;
    
    public GroupObjectEntity toDraw;
    public boolean hide;
    public boolean remove;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, ActionOrProperty.ContextMenuBinding> contextMenuBindings;
    private Map<String, ActionObjectSelector> eventActions;

    public boolean isSelector;

    public boolean optimisticAsync;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public ClassViewType viewType; // assert not null, after initialization
    public String customRenderFunction;
    public static final String SELECT = "<<select>>";
    public static final String NOSELECT = "<<no>>";
    public static final String AUTOSELECT = "<<auto>>";
    public String customChangeFunction;
    public String eventID;

    public Boolean sticky;
    public Boolean sync;

    private String formPath;

    private Pair<Integer, Integer> scriptIndex;
    
    public LocalizedString initCaption = null;
    public String initImage = null;

    public boolean ignoreHasHeaders = false; // hack for property count property
    
    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public String columnsName;
    public ImOrderSet<GroupObjectEntity> columnGroupObjects = SetFact.EMPTYORDER();

    private NFMap<PropertyDrawExtraType, PropertyObjectEntity<?>> propertyExtras = NFFact.map();

    public ImMap<PropertyDrawExtraType, PropertyObjectEntity<?>> getPropertyExtras() {
        return propertyExtras.getMap();
    }
    public PropertyObjectEntity<?> getNFPropertyExtra(PropertyDrawExtraType type, Version version) {
        return propertyExtras.getNFValue(type, version);
    }
    public void setPropertyExtra(PropertyObjectEntity<?> property, PropertyDrawExtraType type, Version version) {
        if(property != null)
            propertyExtras.add(type, property, version);
    }

    public boolean hasDynamicImage() {
        return getPropertyExtra(IMAGE) != null;
    }
    public boolean hasDynamicCaption() {
        return getPropertyExtra(CAPTION) != null;
    }

    public Group group;
    
    public Group getGroup() {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getParent());
    }
    public Group getNFGroup(Version version) {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getNFParent(version));
    }

    public boolean attr;
    public boolean extNull;

    // for pivoting
    public String formula;
    public ImList<PropertyDrawEntity> formulaOperands;

    public PropertyGroupType aggrFunc;
    public ImList<PropertyObjectEntity> lastAggrColumns = ListFact.EMPTY();
    public boolean lastAggrDesc;

    public PropertyDrawEntity quickFilterProperty;

    @IdentityStrongLazy
    public ImSet<PropertyReaderEntity> getQueryProps() {
        MExclSet<PropertyReaderEntity> mResult = SetFact.mExclSet();
        mResult.exclAdd(this);
        PropertyDrawExtraType[] propTypes = {CAPTION, FOOTER, SHOWIF, BACKGROUND, FOREGROUND, IMAGE}; // READONLYIF is absent here
        for (PropertyDrawExtraType type : propTypes) {
            PropertyObjectEntity<?> property = getPropertyExtra(type);
            if (property != null)
                mResult.exclAdd(new PropertyDrawReader(type));
        }
        return mResult.immutable();
    }

    public class PropertyDrawReader implements PropertyReaderEntity {
        private PropertyDrawExtraType type;
        
        public PropertyDrawReader(PropertyDrawExtraType type) {
            this.type = type;    
        }

        @Override
        public Type getReaderType() {
            return getReaderProperty().getType();
        }

        @Override
        public String getReportSID() {
            return PropertyDrawEntity.this.getReportSID() + getReportSuffix();
        }

        @Override
        public PropertyObjectEntity getReaderProperty() {
            return getPropertyExtra(type);
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public String getSID() {
            return PropertyDrawEntity.this.getSID();
        }

        @Override
        public Object getProfiledObject() {
            return getReaderProperty();
        }

        @Override
        public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
            return PropertyDrawEntity.this.getColumnGroupObjects();
        }
        
        @Override
        public String toString() {
            return ThreadLocalContext.localize(type.getText()) + "(" + PropertyDrawEntity.this.toString() + ")";            
        }
        
        protected String getReportSuffix() {
            return type.getReportExtraType().getReportFieldNameSuffix();
        }
    }

    private ActionOrProperty inheritedProperty;

    public PropertyDrawEntity(int ID, String sID, String integrationSID, ActionOrPropertyObjectEntity<P, ?> actionOrProperty, ActionOrProperty inheritedProperty) {
        super(ID);
        setSID(sID);
        setIntegrationSID(integrationSID);
        this.actionOrProperty = actionOrProperty;
        this.inheritedProperty = inheritedProperty;
    }

    public boolean isStaticProperty() {
        return getStaticActionOrProperty() instanceof PropertyObjectEntity;
    }

    public boolean isProperty(FormInstanceContext context) {
        return actionOrProperty instanceof PropertyObjectEntity; // in theory we can also use FormContext if needed
    }

    public PropertyObjectEntity<?> getOrder() {
        return getReaderProperty();
    }

    // for all external calls will set optimistic to true
    public AsyncEventExec getAsyncEventExec(FormInstanceContext context, String actionSID, boolean externalChange) {
        ActionObjectEntity<?> changeAction = getCheckedEventAction(actionSID, context);
        if (changeAction != null) {
            return changeAction.getAsyncEventExec(context, getSecurityProperty(), this, getToDraw(context.entity), optimisticAsync || externalChange);
        }
        return null;
    }

    private boolean isChange(String eventActionSID, FormInstanceContext context) {
        // GROUP_CHANGE can also be in context menu binding (see Property constructor)
        boolean isEdit = CHANGE.equals(eventActionSID) || GROUP_CHANGE.equals(eventActionSID);
        assert isEdit || hasContextMenuBinding(eventActionSID, context) || hasKeyBinding(eventActionSID, context);
        return isEdit;
    }
    
    private boolean checkPermission(Action eventAction, String eventActionSID, FormInstanceContext context, SQLCallable<Boolean> checkReadOnly) throws SQLException, SQLHandledException {
        SecurityPolicy securityPolicy = context.securityPolicy;
        if(EDIT_OBJECT.equals(eventActionSID))
            return securityPolicy == null || securityPolicy.checkPropertyEditObjectsPermission(getSecurityProperty());

        ActionOrProperty securityProperty;
        if (isChange(eventActionSID, context)) {
            if (isReadOnly() || (checkReadOnly != null && checkReadOnly.call())) 
                return false;

            securityProperty = getSecurityProperty(); // will check property itself
        } else { // menu or key binding
            securityProperty = eventAction;
        }

        if(GROUP_CHANGE.equals(eventActionSID) && securityPolicy != null && !securityPolicy.checkPropertyGroupChangePermission(securityProperty))
            return false;

        return securityPolicy == null || securityPolicy.checkPropertyChangePermission(securityProperty, eventAction);
    }

    public ActionObjectEntity<?> getCheckedEventAction(String actionId, FormInstanceContext context) {
        try {
            return getCheckedEventAction(actionId, context, null);
        } catch (SQLException | SQLHandledException e) {
            assert false;
            throw Throwables.propagate(e);
        }
    }
    
    public ActionObjectEntity<?> getCheckedEventAction(String actionId, FormInstanceContext context, SQLCallable<Boolean> checkReadOnly) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> explicitEventAction = getExplicitEventActionEntity(actionId, context);
        ActionObjectEntity<?> eventAction = explicitEventAction != null ?
                explicitEventAction : getDefaultEventAction(actionId, context);

        if (eventAction != null && !checkPermission(eventAction.property, actionId, context, checkReadOnly))
            return null;

        return eventAction;
    }

    private ActionObjectEntity<?> getExplicitEventActionEntity(String actionId, FormInstanceContext context) {
        ActionObjectSelector explicitEventSelector = getExplicitEventAction(actionId);
        ActionObjectEntity<?> explicitEventAction;
        if (explicitEventSelector != null && (explicitEventAction = explicitEventSelector.getAction(context)) != null)
            return explicitEventAction;
        return null;
    }

    private <X extends PropertyInterface> ActionObjectEntity<? extends PropertyInterface> getDefaultEventAction(String actionId, FormInstanceContext context) {
        ActionOrPropertyObjectEntity<X, ?> eventPropertyObject = (ActionOrPropertyObjectEntity<X, ?>) getEventActionOrProperty(context, isChange(actionId, context));
        ActionOrProperty<X> eventProperty = eventPropertyObject.property;
        ImRevMap<X, ObjectEntity> eventMapping = eventPropertyObject.mapping;

        // default implementations for group change
        if (GROUP_CHANGE.equals(actionId)) {
            // checking explicit property handler
            ActionMapImplement<?, X> eventActionImplement = eventProperty.getExplicitEventAction(actionId);
            if(eventActionImplement != null)
                return eventActionImplement.mapObjects(eventMapping);

            // if there is no explicit default handler, then generate one
            ActionObjectEntity<?> eventAction = getCheckedEventAction(CHANGE, context);
            if (eventAction != null)
                return eventAction.getGroupChange(getToDraw(context.entity), getReadOnly());
        } else { // default handler
            ActionMapImplement<?, X> eventActionImplement = eventProperty.getEventAction(actionId, actionId.equals(CHANGE) ? defaultChangeEventScope : null, ListFact.EMPTY(), actionId.equals(CHANGE) ? customChangeFunction : null);
            if (eventActionImplement != null)
                return eventActionImplement.mapObjects(eventMapping);
        }
        return null;
    }

    private ActionObjectSelector getExplicitEventAction(String actionId) {
        if (eventActions != null)
            return eventActions.get(actionId);
        return null;
    }

    public <X extends PropertyInterface> ActionObjectEntity<?> getSelectorAction(FormInstanceContext context) {
        return getSelectorAction(context.entity, isProperty(context) ? (PropertyObjectEntity<? extends PropertyInterface>) getRawCellActionOrProperty() : null);
    }

    @IdentityStrongLazy
    private <X extends PropertyInterface> ActionObjectEntity<?> getSelectorAction(FormEntity entity, PropertyObjectEntity<X> listProperty) {
        GroupObjectEntity groupObject = getToDraw(entity);
        ImSet<ObjectEntity> objects;
        ObjectEntity object;
        ValueClass valueClass;
        if(groupObject != null && (objects = groupObject.getObjects()).size() == 1 &&
                (object = objects.single()).groupTo.viewType.isPanel() && (valueClass = object.baseClass) instanceof CustomClass &&
                listProperty != null) {  //listProperty can be null when SELECTOR is set for an action
            CustomClass customClass = (CustomClass)valueClass;

            ImRevMap<ObjectEntity, PropertyInterface> mapObjects = entity.getObjects().mapRevValues((Supplier<PropertyInterface>) PropertyInterface::new);
            PropertyInterface objectInterface = mapObjects.get(object);

            BaseLogicsModule lm = ThreadLocalContext.getBaseLM();

            LP targetProp = lm.getRequestedValueProperty(customClass);

            ImRevMap<ObjectEntity, PropertyInterface> listMapObjects = mapObjects.removeRev(object);

            // now we don't respect contextFilters (3rd parameter), however later, maybe we can pass it here from formInstance in most call trees
            InputFilterEntity<?, PropertyInterface> filter = entity.getInputFilterAndOrderEntities(object, SetFact.EMPTY(), listMapObjects).first;
            InputPropertyListEntity<X, PropertyInterface> list = new InputPropertyListEntity<>(listProperty.property, listProperty.mapping.innerJoin(listMapObjects));

            // filter orderInterfaces to only used in view and filter
            ImSet<PropertyInterface> usedInterfaces = list.getUsedInterfaces().merge(filter.getUsedInterfaces());
            assert !usedInterfaces.contains(objectInterface);
            ImOrderSet<PropertyInterface> orderUsedInterfaces = usedInterfaces.toOrderSet();

            ImRevMap<PropertyInterface, StaticParamNullableExpr> listMapParamExprs = listMapObjects.reverse().filterIncl(usedInterfaces).mapRevValues(ObjectEntity::getParamExpr);

            // first parameter - object, other used orderInterfaces
            LA<?> dialogInput = lm.addDialogInputAProp(customClass, targetProp, BaseUtils.nvl(defaultChangeEventScope, DEFAULT_SELECTOR_EVENTSCOPE), orderUsedInterfaces, list, objectEntity -> SetFact.singleton(filter.getFilter(objectEntity)), null, groupObject.updateType != UpdateType.NULL, listMapParamExprs);

            ImOrderSet<PropertyInterface> allOrderUsedInterfaces = SetFact.addOrderExcl(SetFact.singletonOrder(objectInterface), orderUsedInterfaces);
            ActionMapImplement<?, PropertyInterface> request = PropertyFact.createRequestAction(allOrderUsedInterfaces.getSet(), dialogInput.getImplement(allOrderUsedInterfaces),
                    object.getSeekPanelAction(lm, targetProp), null);

            PropertyFact.setResetAsync(request.action, new AsyncMapChange<>(null, object, null, null));

            request.action.ignoreChangeSecurityPolicy = true;

            return request.mapObjects(mapObjects.reverse());
        }
        return null;
    }

    public PropertyDrawInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public void setToDraw(GroupObjectEntity toDraw) {
        this.toDraw = toDraw;
    }

    public void setMouseAction(String actionSID) {
        mouseBinding = actionSID;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = new HashMap<>();
        }
        keyBindings.put(ks, actionSID);
    }

    public void setContextMenuAction(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null) {
            contextMenuBindings = new OrderedMap<>();
        }
        contextMenuBindings.put(actionSID, new ActionOrProperty.ContextMenuBinding(caption, null));
    }

    // VALUE, INTERVAL or SELECTOR
    // assert that it is single panel object, VALUE, INTERVAL - Data with classes, SELECT - Object
    public void setSelectorAction(ActionObjectSelector eventAction) {
        setEventAction(CHANGE, eventAction);
        this.isSelector = true;
    }
    public void setEventAction(String actionSID, ActionObjectSelector eventAction) {
        if(actionSID.equals(CHANGE_WYS)) { // CHANGE_WYS, temp check
            startLog("WARNING! CHANGE_WYS is deprecated, use LIST clause in INPUT / DIALOG operator instead " + this);
            return;
        }

        if(eventActions ==null) {
            eventActions = new HashMap<>();
        }
        eventActions.put(actionSID, eventAction);
    }

    public FormSessionScope defaultChangeEventScope = null;
    public static FormSessionScope DEFAULT_ACTION_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_SELECTOR_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_CUSTOMCHANGE_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_VALUES_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_OBJECTS_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_DATACHANGE_EVENTSCOPE = FormSessionScope.NEWSESSION; // since when data changed in the same session, it immediately leads to pessimistic async values which gives a big overhead in most cases

    // should be the same property that is used in getEventAction (because eventActions should be synchronized with the contextMenuBindings)
    public ActionOrProperty<?> getBindingProperty(FormInstanceContext context) {
//        return getInheritedProperty();
        return getEventActionOrProperty(context, false).property;
    }

    public ActionOrPropertyObjectEntity<?, ?> getEventActionOrProperty(FormInstanceContext context, boolean isChange) {
        return isChange ? getCellActionOrProperty(context) : actionOrProperty;
    }

    public Iterable<String> getAllPropertyEventActions(FormInstanceContext context) {
        return BaseUtils.mergeIterables(BaseUtils.mergeIterables(ServerResponse.events, getContextMenuBindings(context).keySet()), getKeyBindings(context).valueIt());
    }
    public OrderedMap<String, ActionOrProperty.ContextMenuBinding> getContextMenuBindings(FormInstanceContext context) {
        ImOrderMap<String, ActionOrProperty.ContextMenuBinding> propertyContextMenuBindings = getBindingProperty(context).getContextMenuBindings();
        if (propertyContextMenuBindings.isEmpty()) {
            return contextMenuBindings;
        }

        OrderedMap<String, ActionOrProperty.ContextMenuBinding> result = new OrderedMap<>();
        for (int i = 0; i < propertyContextMenuBindings.size(); ++i) {
            result.put(propertyContextMenuBindings.getKey(i), propertyContextMenuBindings.getValue(i));
        }

        if (contextMenuBindings != null) {
            result.putAll(contextMenuBindings);
        }

        return result;
    }

    public boolean hasContextMenuBinding(String actionSid, FormInstanceContext context) {
        OrderedMap contextMenuBindings = getContextMenuBindings(context);
        return contextMenuBindings != null && contextMenuBindings.containsKey(actionSid);
    }
    
    public boolean hasKeyBinding(String actionId, FormInstanceContext context) {
        ImMap<KeyStroke, String> keyBindings = getKeyBindings(context);
        return keyBindings != null && keyBindings.containsValue(actionId);
    }

    public ImMap<KeyStroke, String> getKeyBindings(FormInstanceContext context) {
        ImMap<KeyStroke, String> propertyKeyBindings = getBindingProperty(context).getKeyBindings();
        if(keyBindings != null)
            propertyKeyBindings = propertyKeyBindings.merge(MapFact.fromJavaMap(keyBindings), MapFact.override());
        return propertyKeyBindings;
    }

    public String getMouseBinding(FormInstanceContext context) {
        return mouseBinding != null ? mouseBinding : getBindingProperty(context).getMouseBinding();
    }

    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        return columnGroupObjects;
    }
    public void setColumnGroupObjects(String columnsName, ImOrderSet<GroupObjectEntity> columnGroupObjects) {
        this.columnsName = columnsName;
        this.columnGroupObjects = columnGroupObjects;
    }

    public PropertyObjectEntity<?> getPropertyExtra(PropertyDrawExtraType type) {
        return getPropertyExtras().get(type);
    }

    public boolean hasPropertyExtra(PropertyDrawExtraType type) {
        return getPropertyExtra(type) != null;
    }
    
    public PropertyEditType getEditType() {
        return editType;
    }

    public void setEditType(PropertyEditType editType) {
        this.editType = editType;
    }

    public boolean isReadOnly() {
        return editType == PropertyEditType.READONLY;
    }

    public boolean isEditable() {
        return editType == PropertyEditType.EDITABLE;
    }

    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView defaultView) {
        getInheritedProperty().drawOptions.proceedDefaultDesign(propertyView);
    }

    public void proceedDefaultDraw(FormEntity form, Version version) {
        getInheritedProperty().drawOptions.proceedDefaultDraw(this, form, version);
    }

    @Override
    public String toString() {
        return (formPath == null ? "" : formPath) + " property:" + getReflectionActionOrProperty().toString();
    }

    // interactive
    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null? getApplyObject(form, SetFact.EMPTY(), true) :toDraw;
    }

    public GroupObjectEntity getApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getColumnGroupObjects().getSet());
        return form.getApplyObject(getObjectInstances(), excludeGroupObjects);
    }

    public GroupObjectEntity getNFApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns, Version version) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getColumnGroupObjects().getSet());
        return form.getNFApplyObject(getNFObjectInstances(version), excludeGroupObjects, version);
    }

    public ImSet<ObjectEntity> getObjectInstances(Function<PropertyDrawExtraType, PropertyObjectEntity<?>> getProperty) {
        MAddSet<ActionOrPropertyObjectEntity<?, ?>> propertyObjects = SetFact.mAddSet();

        PropertyDrawExtraType[] neededTypes = {CAPTION, FOOTER, SHOWIF, GRIDELEMENTCLASS, VALUEELEMENTCLASS, CAPTIONELEMENTCLASS,
                FONT, BACKGROUND, FOREGROUND, IMAGE, READONLYIF, COMMENT, COMMENTELEMENTCLASS, PLACEHOLDER, PATTERN,
                REGEXP, REGEXPMESSAGE, TOOLTIP, VALUETOOLTIP, PROPERTY_CUSTOM_OPTIONS, CHANGEKEY, CHANGEMOUSE};
        for (PropertyDrawExtraType type : neededTypes) {
            PropertyObjectEntity<?> prop = getProperty.apply(type);
            if (prop != null) {
                propertyObjects.add(prop);
            }
        }
        if(cellProperty != null)
            propertyObjects.add(cellProperty);
        
        MSet<ObjectEntity> mObjects = SetFact.mSet();
        for(int i=0,size=propertyObjects.size();i<size;i++)
            mObjects.addAll(propertyObjects.get(i).getObjectInstances());
        mObjects.addAll(actionOrProperty.getObjectInstances());
        if(toDraw != null)
            mObjects.addAll(toDraw.getObjects());
        return mObjects.immutable();
    }

    @IdentityStartLazy
    public ImSet<ObjectEntity> getObjectInstances() {
        return getObjectInstances(this::getPropertyExtra);
    }
    public ImSet<ObjectEntity> getNFObjectInstances(Version version) {
        return getObjectInstances(type -> getNFPropertyExtra(type, version));
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        return toDraw == null ? getNFApplyObject(form, SetFact.EMPTY(), true, version) : toDraw;
    }

    public boolean isToolbar(FormEntity formEntity) {
        if (viewType != null)
            return viewType.isToolbar();

        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.viewType.isToolbar();
    }

    public boolean isPopup(FormEntity formEntity) {
        if (viewType != null)
            return viewType.isPopup();

        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.viewType.isPopup();
    }

    public boolean isList(FormInstanceContext context) {
        return isList(context.entity);
    }
    public boolean isList(FormEntity formEntity) {
        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.viewType.isList() && (viewType == null || viewType.isList());
    }

    public boolean isNFList(FormEntity formEntity, Version version) {
        GroupObjectEntity toDraw = getNFToDraw(formEntity, version);
        return toDraw != null && toDraw.viewType.isList() && (viewType == null || viewType.isList());
    }

    static public String createSID(String name, List<String> mapping) {
        StringBuilder sidBuilder = new StringBuilder();
        sidBuilder.append(name);
        sidBuilder.append("(");
        for (int i = 0; i < mapping.size(); i++) {
            if (i > 0) {
                sidBuilder.append(",");
            }
            sidBuilder.append(mapping.get(i));
        }
        sidBuilder.append(")");
        return sidBuilder.toString();        
    }

    public static <P extends PropertyInterface> List<String> getMapping(ActionOrPropertyObjectEntity<P, ?> property, ImOrderSet<P> interfaces) {
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces)
            mapping.add(property.mapping.getObject(pi).getSID());
        return mapping;
    }

    public String getFormPath() {
        return formPath;
    }

    public void setFormPath(String formPath) {
        this.formPath = formPath;
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return scriptIndex;
    }

    public void setScriptIndex(Pair<Integer, Integer> scriptIndex) {
        this.scriptIndex = scriptIndex;
    }

    @Override
    public Object getProfiledObject() {
        return this;
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public Type getExternalType(FormInstanceContext context) {
        return getExternalProperty(context).property.getType();
    }

    public Type getPasteType(FormInstanceContext context) {
        return getPasteProperty(context).property.getType();
    }

    public Type getAssertCellType(FormInstanceContext context) {
        return getAssertCellProperty(context).property.getType();
    }

    public Type getStaticType() {
        return getAssertStaticProperty().property.getType();
    }

    @Override
    public Type getReaderType() {
        return getStaticType();
    }

    public Type getImportType() {
        return getStaticType();
    }

    public boolean isPredefinedSwitch() {
        return ((Property<?>)getInheritedProperty()).isPredefinedSwitch();
    }

    public LocalizedString getCaption() {
        return getInheritedProperty().caption;
    }
    public AppServerImage.Reader getImage() {
        return getInheritedProperty().image;
    }

    public boolean isNotNull() {
        return getInheritedProperty().isDrawNotNull();
    }

    public String integrationSID; // hack - can be null for EXPORT FROM orders

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID;
    }

    // IMPORT
    public PropertyObjectEntity getImportProperty() {
        return getAssertStaticProperty();
    }

    public PropertyObjectEntity<?> getCellProperty(FormInstanceContext context) {
        if(isProperty(context))
            return getAssertCellProperty(context);

        if(!context.isNative || getReadOnly() == null) // last check - optimization
            return ActionObjectEntity.TRUE();

        return getActionProperty();
    }

    // deprecated branch for the desktop client
    @IdentityInstanceLazy
    private <X extends PropertyInterface> PropertyObjectEntity<?> getActionProperty() {
        PropertyObjectEntity<X> readOnly = (PropertyObjectEntity<X>) getReadOnly();
        return PropertyFact.createNot(readOnly.property.getImplement()).mapEntityObjects(readOnly.mapping);
    }

    private PropertyObjectEntity<?> getReadOnly() {
        return getPropertyExtras().get(READONLYIF);
    }

    private boolean hasNoGridReadOnly(FormEntity form) {
        PropertyObjectEntity<?> readOnly = getReadOnly();
        return readOnly != null && isList(form) && readOnly.hasNoGridReadOnly(getToDraw(form).getObjects());
    }

    public ActionOrPropertyObjectEntity<?, ?> getStaticActionOrProperty() {
        return actionOrProperty;
    }
    public ActionOrPropertyObjectEntity<?, ?> getReflectionActionOrProperty() {
        return actionOrProperty;
    }

    public static class Select {

        public final PropertyObjectEntity property;

        public final String type;
        public final String elementType;

        public final int length;
        public final int count;
        public final boolean actual;
        public final boolean html;
        public final boolean changeValue;

        public Select(PropertyObjectEntity property, String type, String elementType, int length, int count, boolean actual, boolean html, boolean changeValue) {
            this.property = property;
            this.type = type;
            this.elementType = elementType;
            this.length = length;
            this.count = count;
            this.actual = actual;
            this.html = html;
            this.changeValue = changeValue;
        }
    }

    private boolean hasFooter(FormEntity entity) {
        return isList(entity) && getPropertyExtra(FOOTER) != null;
    }

    public PropertyDrawEntity.Select getCustomSelectProperty(FormInstanceContext context) {
        return getSelectProperty(context);
    }

    @ParamLazy
    public PropertyDrawEntity.Select getSelectProperty(FormInstanceContext context) {
        if(context.isNative)
            return null;

        ActionOrPropertyObjectEntity<?, ?> actionOrProperty = getRawCellActionOrProperty();
        if(actionOrProperty instanceof PropertyObjectEntity) {
            if(customChangeFunction != null)
                return null;

            String elementType = null;

            boolean forceSelect = false;
            Boolean forceFilter = null;

            String custom = getCustomRenderFunction();
            if(custom != null) {
                if(custom.startsWith(SELECT)) {
                    custom = custom.substring(SELECT.length());

                    forceSelect = true;
                    if(!custom.equals(AUTOSELECT)) {
                        elementType = StringUtils.capitalise(custom);

                        forceFilter = elementType.equals("Input");
                    }
                } else
                    return null;
            }

            if(!forceSelect && isGroupCustom(context))
                return null;

            PropertyObjectEntity<?> property = (PropertyObjectEntity<?>) actionOrProperty;

            Property.MapSelect<?> mapSelect;
            boolean changeValue = false;
            ActionObjectEntity<?> explicitChange = getExplicitEventActionEntity(CHANGE, context);
            if(explicitChange != null) {
                // when we have selector, then it's normal for the object to be null, which however can lead to the "closure problem" - current value (it's params / objects) is "pushed" inside the JSON (GROUP) operator, which doesn't support NULL values (so all the options will be "erased")
                GroupObjectEntity toDraw;
                if(!forceSelect && isSelector && (toDraw = getToDraw(context.entity)) != null && toDraw.updateType == UpdateType.NULL) // assert that group object is single panel object
                    return null;

                changeValue = true;
                mapSelect = explicitChange.getSelectProperty(forceSelect, property);
            } else
                mapSelect = property.getSelectProperty(forceSelect);

            PropertyObjectEntity.Select select = ActionOrPropertyObjectEntity.getSelectProperty(context, forceFilter, mapSelect);
            if(select != null) {
                String selectType = null;
                if (select.type == PropertyObjectEntity.Select.Type.MULTI) {
                    selectType = "Multi";
                } else if (select.type == PropertyObjectEntity.Select.Type.NOTNULL) {
                    if (select.count > 1 || forceSelect)
                        selectType = "";
                } else {
                    if (select.count > 0 || forceSelect)
                        selectType = "Null";
                }

                if(selectType != null) {
                    if(elementType == null) {
                        //hasFooter() check is needed to avoid automatically using custom components in the FOOTER
                        if ((!isReadOnly(context) && !hasFooter(context.entity)) || forceSelect) { // we don't have to check hasChangeAction, since canBeChanged is checked in getSelectProperty
                            boolean isMulti = select.type == PropertyObjectEntity.Select.Type.MULTI;
//                            if (select.count == 1 && select.type == PropertyObjectEntity.Select.Type.NOTNULL) {
//                                elementType = "Single";
//                            } else
                            if (select.length <= Settings.get().getMaxLengthForValueButton() && (!isList(context) || select.length <= Settings.get().getMaxLengthForValueButtonGrid())) {
                                elementType = isMulti ? "Button" : "ButtonGroup";
                            } else if (select.count <= Settings.get().getMaxInterfaceStatForValueList() && !isList(context)) {
                                ContainerView container = context.view.get(this).getLayoutParamContainer();
                                if (container != null && container.isHorizontal())
                                    elementType = isMulti ? "Button" : "ButtonGroup";
                                else
                                    elementType = "List";
                            } else if (select.count <= Settings.get().getMaxInterfaceStatForValueDropdown() || (forceSelect && !isMulti)) {
                                elementType = "Dropdown";
                            } else {
                                assert isMulti;
                                elementType = "Input";
                            }
                        }
                    }

                    if (elementType != null)
                        return new Select(select.property, selectType, elementType, select.length, select.count, select.actual, select.html, changeValue);
                }
            }
        }
        return null;
    }

    public boolean isReadOnly(FormInstanceContext context) {
        return isReadOnly() || hasNoGridReadOnly(context.entity);
    }

    public String getCustomRenderFunction(FormInstanceContext context) {
        Select selectProperty = getCustomSelectProperty(context);
        if(selectProperty != null)
            return "select" + selectProperty.type + (selectProperty.html ? "HTML" : "") + (selectProperty.changeValue ? "Value" : "") + selectProperty.elementType;

        String custom = getCustomRenderFunction();
        if(custom != null) {
            if(custom.equals(NOSELECT) || custom.startsWith(SELECT))
                return null;
            return custom;
        }

        return null;
    }
    public boolean isCustomCanBeRenderedInTD(FormInstanceContext context) {
        Select selectProperty = getCustomSelectProperty(context);
        if(selectProperty != null)
            return true; // we have "manual" _wrapElement in select.js

        return false;
    }

    public boolean isCustomNeedPlaceholder(FormInstanceContext context) {
        Select selectProperty = getCustomSelectProperty(context);
        if(selectProperty != null && (selectProperty.elementType.equals("Dropdown") || selectProperty.elementType.equals("Input")))
            return true; // we have "manual" _wrapElement in select.js

        return false;
    }

    public boolean isCustomNeedReadonly(FormInstanceContext context) {
        Select selectProperty = getCustomSelectProperty(context);
        if(selectProperty != null && ((selectProperty.elementType.equals("List") || selectProperty.elementType.startsWith("Button"))))
            return true; // we use setReadonlyFnc for the _option function in select.js

        return false;
    }

    private String getCustomRenderFunction() {
        if(customRenderFunction != null)
            return customRenderFunction;
        return getInheritedProperty().getCustomRenderFunction();
    }


    public PropertyObjectEntity<?> cellProperty; // maybe later should be implemented as PropertyExtra
    private ActionOrPropertyObjectEntity<?, ?> getRawCellActionOrProperty() {
        if(cellProperty != null)
            return cellProperty;

        return actionOrProperty;
    }

    // INTERACTIVE (NON-STATIC) USAGES
    public ActionOrPropertyObjectEntity<?, ?> getCellActionOrProperty(FormInstanceContext context) {
        Select select = getSelectProperty(context);
        if(select != null)
            return select.property;

        return getRawCellActionOrProperty();
    }

    public PropertyObjectEntity<?> getFilterProperty(FormInstanceContext context) {
        return getAssertValueProperty(context);
    }

    public PropertyObjectEntity<?> getPasteProperty(FormInstanceContext context) {
        return getAssertValueProperty(context);
    }

    // actual value
    public PropertyObjectEntity<?> getAssertValueProperty(FormInstanceContext context) {
        assert isProperty(context);
        return (PropertyObjectEntity<?>) actionOrProperty;
    }

    public PropertyObjectEntity<?> getAssertCellProperty(FormInstanceContext context) {
        assert isProperty(context);
        return (PropertyObjectEntity<?>) getCellActionOrProperty(context);
    }

    public boolean isDifferentValue(FormInstanceContext context) {
        return isProperty(context) && !getAssertValueProperty(context).equalsMap(getAssertCellProperty(context));
    }
    public PropertyObjectEntity<?> getExternalProperty(FormInstanceContext context) {
        return getAssertValueProperty(context);
    }


    // EXPORT / JSON + ORDERS IN INPUT / SELECTOR
    @Override
    public PropertyObjectEntity getReaderProperty() {
        return getAssertStaticProperty();
    }

    // EXPORT / JSON / IMPORT
    public PropertyObjectEntity<?> getAssertStaticProperty() {
        assert isStaticProperty();
        return (PropertyObjectEntity<?>) getStaticActionOrProperty();
    }

    // presentation info, probably should be merged with inheritDrawOptions mechanism
    public ActionOrProperty getInheritedProperty() {
        return inheritedProperty;
    }

    public ActionOrProperty getSecurityProperty() {
        return getInheritedProperty();
    }

    // for debug purposes
    public ActionOrProperty getReflectionBindingProperty() {
        return getInheritedProperty();
    }

    @Override
    public String getReportSID() {
        return getSID();
    }

    public boolean isGroupCustom(FormInstanceContext context) {
        GroupObjectEntity toDraw;
        return isList(context) && (toDraw = getToDraw(context.entity)) != null && toDraw.isCustom();
    }

    public boolean isGroupSimpleState(FormInstanceContext context) {
        GroupObjectEntity toDraw;
        return isList(context) && (toDraw = getToDraw(context.entity)) != null && toDraw.isSimpleState();
    }

    public boolean needFile(FormInstanceContext context) {
        return getCustomRenderFunction(context) != null || isGroupCustom(context);
    }

    public boolean needImage(FormInstanceContext context) {
        return getCustomRenderFunction(context) != null || isGroupSimpleState(context);
    }

    // should match PropertyDrawEntity.isPredefinedImage
    public boolean isPredefinedImage() {
        String sid = getIntegrationSID();
        return sid != null && sid.equals("image");
    }

}
