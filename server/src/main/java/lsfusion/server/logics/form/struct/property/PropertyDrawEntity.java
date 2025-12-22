package lsfusion.server.logics.form.struct.property;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.identity.IDGenerator;
import lsfusion.server.base.AppServerImage;
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
import lsfusion.server.base.version.interfaces.NFOrderMap;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.change.ActionObjectSelector;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.action.input.InputPropertyListEntity;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawViewOrPivotColumn;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.IdentityEntity;
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
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.logics.form.interactive.design.property.PropertyDrawView.hasNoCaption;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;

public class PropertyDrawEntity<P extends PropertyInterface, AddParent extends IdentityEntity<AddParent, ?>> extends IdentityEntity<PropertyDrawEntity<P, AddParent>, AddParent> implements PropertyDrawEntityOrPivotColumn<PropertyDrawEntity<P, AddParent>>, Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private final NFProperty<PropertyEditType> editType = NFFact.property();
    
    public ActionOrPropertyObjectEntity<P, ?, ?> actionOrProperty;
    
    private final NFProperty<GroupObjectEntity> toDraw = NFFact.property();
    private final NFProperty<Boolean> hide = NFFact.property();
    private final NFProperty<Boolean> remove = NFFact.property();

    private final NFOrderMap<String, ActionOrProperty.ContextMenuBinding> contextMenuBindings = NFFact.orderMap();
    private final NFMap<String, ActionObjectSelector> eventActions = NFFact.map();

    private final NFProperty<Boolean> isSelector = NFFact.property();

    private final NFProperty<Boolean> optimisticAsync = NFFact.property();

    private final NFProperty<Boolean> askConfirm = NFFact.property();
    private final NFProperty<String> askConfirmMessage = NFFact.property();

    private final NFProperty<ClassViewType> viewType = NFFact.property(); // assert not null, after initialization
    private final NFProperty<String> customRenderFunction = NFFact.property();
    public static final String SELECT = "<<select>>";
    public static final String NOSELECT = "<<no>>";
    public static final String AUTOSELECT = "<<auto>>";
    private final NFProperty<String> customChangeFunction = NFFact.property();
    private final NFProperty<String> eventID = NFFact.property();

    private final NFProperty<Boolean> sticky = NFFact.property();
    private final NFProperty<Boolean> sync = NFFact.property();

    // derived from debugPoint (see getFormPath() / getScriptIndex())
    
    private final NFProperty<Boolean> ignoreHasHeaders = NFFact.property(); // hack for property count property

    private final NFProperty<LocalizedString> caption = NFFact.property();
    private final NFProperty<AppServerImage.Reader> image = NFFact.property();

    public LocalizedString getCaption() {
        LocalizedString captionValue = caption.get();
        if (captionValue != null)
            return captionValue;

        return getInheritedProperty().caption;
    }

    public void setCaption(LocalizedString value, Version version) {
        caption.set(value,version);
    }

    // we return to the client null, if we're sure that caption is always empty (so we don't need to draw label)
    public String getDrawCaption() {
        LocalizedString caption = getCaption();
        if(hasNoCaption(caption, getPropertyExtra(CAPTION), view.getElementClass()))
            return null;

        return ThreadLocalContext.localize(caption);
    }

    public void setImage(String image, Version version) {
        setImage(AppServerImage.createPropertyImage(image, this), version);
    }

    public void setImage(AppServerImage.Reader value,Version version) {
        image.set(value,version);
    }

    public AppServerImage getImage(ConnectionContext context) {
        AppServerImage.Reader img = image.get();
        if(img != null)
            return img.get(context);

        AppServerImage.Reader entityImage = getInheritedProperty().image;
        if(entityImage != null)
            return entityImage.get(context);

        return getDefaultImage(context);
    }

    private AppServerImage getDefaultImage(ConnectionContext context) {
        return ActionOrProperty.getDefaultImage(AppServerImage.AUTO, getAutoName(), Settings.get().getDefaultPropertyImageRankingThreshold(), Settings.get().isDefaultPropertyImage(), context);
    }

    public AppServerImage.AutoName getAutoName() {
        return AppServerImage.getAutoName(this::getCaption, getInheritedProperty()::getName);
    }

    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    private final NFProperty<String> columnsName = NFFact.property();
    private final NFProperty<ImOrderSet<GroupObjectEntity>> columnGroupObjects = NFFact.property();

    private final NFMap<PropertyDrawExtraType, PropertyObjectEntity> propertyExtras = NFFact.map();

    public ImMap<PropertyDrawExtraType, PropertyObjectEntity> getPropertyExtras() {
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

    private final NFProperty<Group> group = NFFact.property();
    private final NFProperty<Boolean> attr = NFFact.property();
    private final NFProperty<Boolean> extNull = NFFact.property();

    // for pivoting
    private final NFProperty<String> formula = NFFact.property();
    private final NFProperty<ImList<PropertyDrawEntity>> formulaOperands = NFFact.property();

    private final NFProperty<PropertyGroupType> aggrFunc = NFFact.property();
    private final NFProperty<ImList<PropertyObjectEntity>> lastAggrColumns = NFFact.property();
    private final NFProperty<Boolean> lastAggrDesc = NFFact.property();

    private final NFProperty<PropertyDrawEntity> quickFilterProperty = NFFact.property();

    public Group getGroup() {
        Group explicit = group.get();
        return (explicit != null ? (explicit == Group.NOGROUP ? null : explicit) : getInheritedProperty().getParent());
    }
    public Group getNFGroup(Version version) {
        Group explicit = group.getNF(version);
        return (explicit != null ? (explicit == Group.NOGROUP ? null : explicit) : getInheritedProperty().getNFParent(version, true));
    }
    public void setGroup(Group group, FormEntity form, Version version) {
        this.group.set(group, version);

        form.updatePropertyDraw(this, version);
    }

    public boolean isAttr() {
        Boolean value = attr.get();
        return value != null && value;
    }
    public void setAttr(boolean attr, Version version) {
        this.attr.set(attr, version);
    }

    public boolean isExtNull() {
        Boolean value = extNull.get();
        return value != null && value;
    }
    public void setExtNull(boolean extNull, Version version) {
        this.extNull.set(extNull, version);
    }

    public String getFormula() {
        return formula.get();
    }
    public void setFormula(String formula, Version version) {
        this.formula.set(formula, version);
    }
    public ImList<PropertyDrawEntity> getFormulaOperands() {
        return formulaOperands.get();
    }
    public void setFormulaOperands(ImList<PropertyDrawEntity> formulaOperands, Version version) {
        this.formulaOperands.set(formulaOperands, version);
    }

    public PropertyGroupType getAggrFunc() {
        return aggrFunc.get();
    }
    public void setAggrFunc(PropertyGroupType aggrFunc, Version version) {
        this.aggrFunc.set(aggrFunc, version);
    }
    public ImList<PropertyObjectEntity> getLastAggrColumns() {
        ImList<PropertyObjectEntity> value = lastAggrColumns.get();
        return value != null ? value : ListFact.EMPTY();
    }
    public void setLastAggrColumns(ImList<PropertyObjectEntity> lastAggrColumns, Version version) {
        this.lastAggrColumns.set(lastAggrColumns, version);
    }
    public boolean isLastAggrDesc() {
        Boolean value = lastAggrDesc.get();
        return value != null && value;
    }
    public void setLastAggrDesc(boolean lastAggrDesc, Version version) {
        this.lastAggrDesc.set(lastAggrDesc, version);
    }

    public PropertyDrawEntity getQuickFilterProperty() {
        return quickFilterProperty.get();
    }
    public void setQuickFilterProperty(PropertyDrawEntity quickFilterProperty, Version version) {
        this.quickFilterProperty.set(quickFilterProperty, version);
    }

    private final FormEntity.ExProperty activeProperty;
    public Property<?> getNFActiveProperty(Version version) {
        return activeProperty.getNF(version);
    }
    public Property<?> getActiveProperty() {
        return activeProperty.get();
    }

    public void updateActiveProperty(DataSession session, Boolean value) throws SQLException, SQLHandledException {
        Property<?> activeProperty = getActiveProperty();
        if(activeProperty != null)
            activeProperty.change(session, value);
    }

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

    @IdentityStrongLazy
    public PropertyReaderEntity getShowIfProp() {
        PropertyObjectEntity showIf = getPropertyExtra(PropertyDrawExtraType.SHOWIF);
        return showIf != null ? new PropertyDrawEntity.PropertyDrawReader(PropertyDrawExtraType.SHOWIF) : null;
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

    @Override
    protected String getDefaultSIDPrefix() {
        return "prop";
    }

    public PropertyDrawEntity(IDGenerator ID, String sID, ActionOrPropertyObjectEntity<P, ?, ?> actionOrProperty, ActionOrProperty inheritedProperty, DebugInfo.DebugPoint debugPoint) {
        super(ID, sID, debugPoint);
        this.actionOrProperty = actionOrProperty;
        this.inheritedProperty = inheritedProperty;

        this.activeProperty = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("ACTIVE PROPERTY", this, LogicalClass.instance));
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
            return changeAction.getAsyncEventExec(context, getSecurityProperty(), this, getToDraw(context.entity), isOptimisticAsync() || externalChange);
        }
        return null;
    }

    private boolean isChange(String eventActionSID, FormInstanceContext context) {
        // GROUP_CHANGE can also be in context menu binding (see Property constructor)
        boolean isEdit = CHANGE.equals(eventActionSID) || GROUP_CHANGE.equals(eventActionSID);
        assert isEdit || hasContextMenuBinding(eventActionSID, context);
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
        ActionObjectSelector<?> explicitEventSelector = getExplicitEventAction(actionId);
        ActionObjectEntity<?> explicitEventAction;
        if (explicitEventSelector != null && (explicitEventAction = explicitEventSelector.getAction(context)) != null)
            return explicitEventAction;
        return null;
    }

    private <X extends PropertyInterface> ActionObjectEntity<? extends PropertyInterface> getDefaultEventAction(String actionId, FormInstanceContext context) {
        ActionOrPropertyObjectEntity<X, ?, ?> eventPropertyObject = (ActionOrPropertyObjectEntity<X, ?, ?>) getEventActionOrProperty(context, isChange(actionId, context));
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
            ActionMapImplement<?, X> eventActionImplement = eventProperty.getEventAction(actionId, actionId.equals(CHANGE) ? getDefaultChangeEventScope() : null, ListFact.EMPTY(), actionId.equals(CHANGE) ? getCustomChangeFunction() : null);
            if (eventActionImplement != null)
                return eventActionImplement.mapObjects(eventMapping);
        }
        return null;
    }

    public ActionObjectSelector<?> getExplicitEventAction(String actionId) {
        return eventActions.getMap().get(actionId);
    }

    public static class SelectorAction implements ActionObjectSelector<SelectorAction> {

        private final Function<FormInstanceContext, ActionObjectEntity<?>> getAction;

        public SelectorAction(Function<FormInstanceContext, ActionObjectEntity<?>> getAction) {
            this.getAction = getAction;
        }

        @Override
        public ActionObjectEntity<?> getAction(FormInstanceContext context) {
            return getAction.apply(context);
        }

        @Override
        public SelectorAction get(ObjectMapping mapping) {
            return new SelectorAction(formInstanceContext -> mapping.get((ActionObjectEntity) getAction.apply(formInstanceContext)));
        }
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
        if (groupObject != null && (objects = groupObject.getObjects()).size() == 1 && ((GroupObjectEntity) (object = objects.single()).groupTo).isPanel() && (valueClass = object.baseClass) instanceof CustomClass &&
                listProperty != null) {  //listProperty can be null when SELECTOR is set for an action
            CustomClass customClass = (CustomClass) valueClass;

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
            LA<?> dialogInput = lm.addDialogInputAProp(customClass, targetProp, BaseUtils.nvl(getDefaultChangeEventScope(), DEFAULT_SELECTOR_EVENTSCOPE), orderUsedInterfaces, list, objectEntity -> SetFact.singleton(filter.getFilter(objectEntity)), null, groupObject.getUpdateType() != UpdateType.NULL, listMapParamExprs);

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

    public void setToDraw(GroupObjectEntity toDraw, FormEntity form, Version version) {
        this.toDraw.set(toDraw, version);

        form.updatePropertyDraw(this, version);
    }

    public void setViewType(ClassViewType viewType, FormEntity form, Version version) {
        this.viewType.set(viewType, version);

        form.updatePropertyDraw(this, version);
    }

    public ClassViewType getNFViewType(Version version) {
        return viewType.getNF(version);
    }

    public ClassViewType getViewType() {
        return viewType.get();
    }

    public GroupObjectEntity getToDraw() {
        return toDraw.get();
    }

    public GroupObjectEntity getNFToDraw(Version version) {
        return toDraw.getNF(version);
    }

    public void setHide(boolean hide, Version version) {
        this.hide.set(hide, version);
    }

    public boolean isHide() {
        Boolean value = hide.get();
        return value != null && value;
    }

    public void setRemove(boolean remove, Version version) {
        this.remove.set(remove, version);
    }

    public boolean isRemove() {
        Boolean value = remove.get();
        return value != null && value;
    }

    public boolean isIgnoreHasHeaders() {
        Boolean value = ignoreHasHeaders.get();
        return value != null && value;
    }

    public void setIgnoreHasHeaders(boolean ignoreHasHeaders, Version version) {
        this.ignoreHasHeaders.set(ignoreHasHeaders, version);
    }

    public void setAskConfirm(Boolean askConfirm, Version version) {
        this.askConfirm.set(askConfirm, version);
    }

    public Boolean getAskConfirm() {
        return askConfirm.get();
    }

    public void setAskConfirmMessage(String askConfirmMessage, Version version) {
        this.askConfirmMessage.set(askConfirmMessage, version);
    }

    public String getAskConfirmMessage() {
        return askConfirmMessage.get();
    }

    public String getColumnsName() {
        return columnsName.get();
    }

    public void setColumnsName(String columnsName, Version version) {
        this.columnsName.set(columnsName, version);
    }

    public boolean isSelector() {
        Boolean value = isSelector.get();
        return value != null && value;
    }

    public boolean isOptimisticAsync() {
        Boolean value = optimisticAsync.get();
        return value != null && value;
    }

    public String getEventID() {
        return eventID.get();
    }

    public void setCustomRenderFunction(String customRenderFunction, Version version) {
        this.customRenderFunction.set(customRenderFunction, version);
    }

    public void setCustomChangeFunction(String customChangeFunction, Version version) {
        this.customChangeFunction.set(customChangeFunction, version);
    }

    public void setOptimisticAsync(boolean optimisticAsync, Version version) {
        this.optimisticAsync.set(optimisticAsync, version);
    }

    public void setEventID(String eventID, Version version) {
        this.eventID.set(eventID, version);
    }

    public void setSticky(Boolean sticky, Version version) {
        this.sticky.set(sticky, version);
    }

    public Boolean getSticky() {
        return sticky.get();
    }

    public void setSync(Boolean sync, Version version) {
        this.sync.set(sync, version);
    }

    public Boolean getSync() {
        return sync.get();
    }

    public void setContextMenuAction(String actionSID, LocalizedString caption, Version version) {
        contextMenuBindings.add(actionSID, new ActionOrProperty.ContextMenuBinding(caption, null), version);
    }

    // VALUE, INTERVAL or SELECTOR
    // assert that it is single panel object, VALUE, INTERVAL - Data with classes, SELECT - Object
    public void setSelectorAction(ActionObjectSelector<?> eventAction, Version version) {
        setEventAction(CHANGE, eventAction, version);
        this.isSelector.set(true, version);
    }
    public void setEventAction(String actionSID, ActionObjectSelector<?> eventAction, Version version) {
        if(actionSID.equals(CHANGE_WYS)) { // CHANGE_WYS, temp check
            startLog("WARNING! CHANGE_WYS is deprecated, use LIST clause in INPUT / DIALOG operator instead " + this);
            return;
        }

        eventActions.add(actionSID, eventAction, version);
    }

    private final NFProperty<FormSessionScope> defaultChangeEventScope = NFFact.property();
    public static FormSessionScope DEFAULT_ACTION_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_SELECTOR_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_CUSTOMCHANGE_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_VALUES_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_OBJECTS_EVENTSCOPE = FormSessionScope.OLDSESSION;
    public static FormSessionScope DEFAULT_DATACHANGE_EVENTSCOPE = FormSessionScope.NEWSESSION; // since when data changed in the same session, it immediately leads to pessimistic async values which gives a big overhead in most cases

    public FormSessionScope getDefaultChangeEventScope() {
        return defaultChangeEventScope.get();
    }

    public void setDefaultChangeEventScope(FormSessionScope defaultChangeEventScope, Version version) {
        this.defaultChangeEventScope.set(defaultChangeEventScope, version);
    }

    // should be the same property that is used in getEventAction (because eventActions should be synchronized with the contextMenuBindings)
    public ActionOrProperty<?> getBindingProperty(FormInstanceContext context) {
//        return getInheritedProperty();
        return getEventActionOrProperty(context, false).property;
    }

    public ActionOrPropertyObjectEntity<?, ?, ?> getEventActionOrProperty(FormInstanceContext context, boolean isChange) {
        return isChange ? getCellActionOrProperty(context) : actionOrProperty;
    }

    public Iterable<String> getAllPropertyEventActions(FormInstanceContext context) {
        return BaseUtils.mergeIterables(ServerResponse.events, getContextMenuBindings(context).keyIt());
    }
    public ImOrderMap<String, ActionOrProperty.ContextMenuBinding> getContextMenuBindings(FormInstanceContext context) {
       return getContextMenuBindings().mergeOrder(getBindingProperty(context).getContextMenuBindings());
    }

    private ImOrderMap<String, ActionOrProperty.ContextMenuBinding> getContextMenuBindings() {
        return contextMenuBindings.getListMap();
    }

    private static OrderedMap<String, ActionOrProperty.ContextMenuBinding> toOrderedMap(ImOrderMap<String, ActionOrProperty.ContextMenuBinding> map) {
        OrderedMap<String, ActionOrProperty.ContextMenuBinding> result = new OrderedMap<>();
        for (int i = 0; i < map.size(); ++i)
            result.put(map.getKey(i), map.getValue(i));
        return result;
    }

    public boolean hasContextMenuBinding(String actionSid, FormInstanceContext context) {
        return getContextMenuBindings(context).containsKey(actionSid);
    }

    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        ImOrderSet<GroupObjectEntity> value = columnGroupObjects.get();
        return value != null ? value : SetFact.EMPTYORDER();
    }

    public ImOrderSet<GroupObjectEntity> getNFColumnGroupObjects(Version version) {
        ImOrderSet<GroupObjectEntity> value = columnGroupObjects.getNF(version);
        return value != null ? value : SetFact.EMPTYORDER();
    }

    public void setColumnGroupObjects(String columnsName, ImOrderSet<GroupObjectEntity> columnGroupObjects, FormEntity form, Version version) {
        this.columnsName.set(columnsName, version);
        this.columnGroupObjects.set(columnGroupObjects, version);

        form.updatePropertyDraw(this, version);
    }

    public PropertyObjectEntity<?> getPropertyExtra(PropertyDrawExtraType type) {
        return getPropertyExtras().get(type);
    }

    public boolean hasPropertyExtra(PropertyDrawExtraType type) {
        return getPropertyExtra(type) != null;
    }
    
    public PropertyEditType getEditType() {
        PropertyEditType value = editType.get();
        return value != null ? value : PropertyEditType.EDITABLE;
    }

    public PropertyEditType getNFEditType(Version version) {
        PropertyEditType value = editType.getNF(version);
        return value != null ? value : PropertyEditType.EDITABLE;
    }

    public void setEditType(PropertyEditType editType, Version version) {
        this.editType.set(editType, version);
    }

    public boolean isReadOnly() {
        return getEditType() == PropertyEditType.READONLY;
    }

    public boolean isEditable() {
        return getEditType() == PropertyEditType.EDITABLE;
    }

    public void proceedDefaultDraw(FormEntity form, Version version) {
        getInheritedProperty().drawOptions.proceedDefaultDraw(this, form, version);
    }

    @Override
    public String toString() {
        String path = getFormPath();
        return ThreadLocalContext.localize(getCaption()) + " " + (path == null ? "" : path) + " property:" + getReflectionActionOrProperty();
    }

    // interactive
    @Override
    public GroupObjectEntity getToDraw(FormEntity form) {
        GroupObjectEntity explicit = getToDraw();
        return explicit == null ? getApplyObject(form, SetFact.EMPTY(), true) : explicit;
    }

    @Override
    public PropertyDrawViewOrPivotColumn getPropertyDrawViewOrPivotColumn(FormView formView) {
        return formView.get(this);
    }

    public GroupObjectEntity getApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getColumnGroupObjects().getSet());
        return form.getApplyObject(getObjectInstances(), excludeGroupObjects);
    }

    public GroupObjectEntity getNFApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns, Version version) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getNFColumnGroupObjects(version).getSet());
        return form.getNFApplyObject(getNFObjectInstances(version), excludeGroupObjects, version);
    }

    public ImSet<ObjectEntity> getObjectInstances(Function<PropertyDrawExtraType, PropertyObjectEntity<?>> getProperty, Supplier<GroupObjectEntity> getToDraw) {
        MAddSet<ActionOrPropertyObjectEntity<?, ?, ?>> propertyObjects = SetFact.mAddSet();

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
        GroupObjectEntity toDraw = getToDraw.get();
        if(toDraw != null)
            mObjects.addAll(toDraw.getObjects());
        return mObjects.immutable();
    }

    @IdentityStartLazy
    public ImSet<ObjectEntity> getObjectInstances() {
        return getObjectInstances(this::getPropertyExtra, this::getToDraw);
    }
    public ImSet<ObjectEntity> getNFObjectInstances(Version version) {
        return getObjectInstances(type -> getNFPropertyExtra(type, version), () -> getNFToDraw(version));
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        GroupObjectEntity explicit = getNFToDraw(version);
        return explicit == null ? getNFApplyObject(form, SetFact.EMPTY(), true, version) : explicit;
    }

    public boolean isNFToolbar(FormEntity formEntity, Version version) {
        ClassViewType explicitViewType = getNFViewType(version);
        if (explicitViewType != null)
            return explicitViewType.isToolbar();

        GroupObjectEntity toDraw = getNFToDraw(formEntity, version);
        return toDraw != null && toDraw.getNFViewType(version).isToolbar();
    }

    public boolean isNFPopup(FormEntity formEntity, Version version) {
        ClassViewType explicitViewType = getNFViewType(version);
        if (explicitViewType != null)
            return explicitViewType.isPopup();

        GroupObjectEntity toDraw = getNFToDraw(formEntity, version);
        return toDraw != null && toDraw.getNFViewType(version).isPopup();
    }

    public boolean isList(FormInstanceContext context) {
        return isList(context.entity);
    }
    public boolean isList(FormEntity formEntity) {
        GroupObjectEntity toDraw = getToDraw(formEntity);
        ClassViewType explicitViewType = getViewType();
        return toDraw != null && toDraw.getViewType().isList() && (explicitViewType == null || explicitViewType.isList());
    }

    public boolean isNFList(FormEntity formEntity, Version version) {
        GroupObjectEntity toDraw = getNFToDraw(formEntity, version);
        ClassViewType explicitViewType = getNFViewType(version);
        return toDraw != null && toDraw.getNFViewType(version).isList() && (explicitViewType == null || explicitViewType.isList());
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
    static public String getDefaultIntegrationSID(String sID) {
        int nameEnd = sID.indexOf("(");
        if(nameEnd >= 0)
            return sID.substring(0, nameEnd);

        return sID;
    }

    public static <P extends PropertyInterface> List<String> getMapping(ActionOrPropertyObjectEntity<P, ?, ?> property, ImOrderSet<P> interfaces) {
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces)
            mapping.add(property.mapping.getObject(pi).getSID());
        return mapping;
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return debugPoint != null ? Pair.create(debugPoint.getScriptLine(), debugPoint.offset) : null;
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

    public boolean isNotNull() {
        return getInheritedProperty().isDrawNotNull();
    }

    public static String NOEXTID = "NOEXTID";
    public NFProperty<String> integrationSID = NFFact.property(); // hack - can be null for EXPORT FROM orders

    public void setIntegrationSID(String integrationSID, Version version) {
        assert integrationSID != null;
        this.integrationSID.set(integrationSID, version);
    }

    public String getIntegrationSID() {
        String integrationSID = this.integrationSID.get();
        if(integrationSID != null)
            return integrationSID.equals(NOEXTID) ? null : integrationSID;

        return getDefaultIntegrationSID(getSID());
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

    public ActionOrPropertyObjectEntity<?, ?, ?> getStaticActionOrProperty() {
        return actionOrProperty;
    }
    public ActionOrPropertyObjectEntity<?, ?, ?> getReflectionActionOrProperty() {
        return actionOrProperty;
    }

    public static class Select {

        public final PropertyObjectEntity property;

        public final String type;
        public final String elementType;

        public final long length;
        public final long count;
        public final boolean actual;
        public final boolean html;
        public final boolean changeValue;

        public Select(PropertyObjectEntity property, String type, String elementType, long length, long count, boolean actual, boolean html, boolean changeValue) {
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

        ActionOrPropertyObjectEntity<?, ?, ?> actionOrProperty = getRawCellActionOrProperty();
        if(actionOrProperty instanceof PropertyObjectEntity) {
            if(getCustomChangeFunction() != null)
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
                if(!forceSelect && isSelector() && (toDraw = getToDraw(context.entity)) != null && toDraw.getUpdateType() == UpdateType.NULL) // assert that group object is single panel object
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

    private String getCustomChangeFunction() {
        return customChangeFunction.get();
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
        String custom = customRenderFunction.get();
        if(custom != null)
            return custom;
        return getInheritedProperty().getCustomRenderFunction();
    }


    public PropertyObjectEntity<?> cellProperty; // maybe later should be implemented as PropertyExtra
    private ActionOrPropertyObjectEntity<?, ?, ?> getRawCellActionOrProperty() {
        if(cellProperty != null)
            return cellProperty;

        return actionOrProperty;
    }

    // INTERACTIVE (NON-STATIC) USAGES
    public ActionOrPropertyObjectEntity<?, ?, ?> getCellActionOrProperty(FormInstanceContext context) {
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

    public PropertyDrawView<P, AddParent> view;

    // copy-constructor
    protected PropertyDrawEntity(PropertyDrawEntity<P, AddParent> src, ObjectMapping mapping) {
        super(src, mapping);

        view = mapping.get(src.view);
        inheritedProperty = src.inheritedProperty;
        actionOrProperty = mapping.get((ActionOrPropertyObjectEntity)src.actionOrProperty);
        cellProperty = mapping.get((PropertyObjectEntity) src.cellProperty);

        activeProperty = mapping.get(src.activeProperty);

        addParent = mapping.get(src.addParent);
        addChild = src.addChild;
    }

    @Override
    public void extend(PropertyDrawEntity<P, AddParent> src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(integrationSID, src.integrationSID);

        mapping.sets(caption, src.caption);
        mapping.sets(image, src.image);

        mapping.sets(editType, src.editType);
        mapping.sets(hide, src.hide);
        mapping.sets(remove, src.remove);

        mapping.set(toDraw, src.toDraw);

        mapping.adds(contextMenuBindings, src.contextMenuBindings);

        mapping.sets(isSelector, src.isSelector);
        mapping.sets(optimisticAsync, src.optimisticAsync);
        mapping.sets(askConfirm, src.askConfirm);
        mapping.sets(askConfirmMessage, src.askConfirmMessage);

        mapping.sets(viewType, src.viewType);
        mapping.sets(customRenderFunction, src.customRenderFunction);
        mapping.sets(customChangeFunction, src.customChangeFunction);
        mapping.sets(eventID, src.eventID);

        mapping.sets(sticky, src.sticky);
        mapping.sets(sync, src.sync);

        mapping.sets(ignoreHasHeaders, src.ignoreHasHeaders);

        mapping.sets(columnsName, src.columnsName);
        mapping.seto(columnGroupObjects, src.columnGroupObjects);

        mapping.sets(group, src.group);
        mapping.sets(attr, src.attr);
        mapping.sets(extNull, src.extNull);

        mapping.sets(formula, src.formula);
        mapping.setl(formulaOperands, src.formulaOperands);

        mapping.sets(aggrFunc, src.aggrFunc);
        mapping.setl(lastAggrColumns, src.lastAggrColumns);
        mapping.sets(lastAggrDesc, src.lastAggrDesc);

        mapping.sets(defaultChangeEventScope, src.defaultChangeEventScope);

        mapping.set(quickFilterProperty, src.quickFilterProperty);
    }

    @Override
    public void add(PropertyDrawEntity<P, AddParent> src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.add(propertyExtras, src.propertyExtras);
        mapping.add(eventActions, src.eventActions);
    }

    public IdentityEntity addParent;
    public Function<IdentityEntity, PropertyDrawEntity<P, ?>> addChild;
    public <PC extends IdentityEntity> void setAddParent(PC addParent, Function<PC, PropertyDrawEntity<P, ?>> addChild) {
        this.addParent = addParent;
        this.addChild = (Function<IdentityEntity, PropertyDrawEntity<P, ?>>) addChild;
    }
    @Override
    public AddParent getAddParent(ObjectMapping mapping) {
        // if(!mapping.extend)
        // merge default elements - form:manage, group: count it is not clear when has to be false
        return (AddParent) addParent;
    }
    @Override
    public PropertyDrawEntity<P, AddParent> getAddChild(AddParent addParent, ObjectMapping mapping) {
        return (PropertyDrawEntity<P, AddParent>) addChild.apply(addParent);
    }
    @Override
    public PropertyDrawEntity<P, AddParent> copy(ObjectMapping mapping) {
        return new PropertyDrawEntity<>(this, mapping);
    }
}
