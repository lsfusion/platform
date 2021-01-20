package lsfusion.server.logics.form.struct.property;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.ExplicitAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.logics.form.struct.property.PropertyDrawExtraType.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;
    
    private final ActionOrPropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<String, ActionObjectEntity<?>> eventActions;

    public boolean optimisticAsync;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public Boolean shouldBeLast;
    public ClassViewType viewType; // assert not null, after initialization
    public String eventID;

    private String formPath;

    private Pair<Integer, Integer> scriptIndex;
    
    public LocalizedString initCaption = null; // чисто техническая особенность реализации
    
    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public String columnsName;
    public ImOrderSet<GroupObjectEntity> columnGroupObjects = SetFact.EMPTYORDER();

    private Object propertyExtras = MapFact.mMap(MapFact.override());
    private boolean finalizedPropertyExtras;

    @LongMutable
    public ImMap<PropertyDrawExtraType, PropertyObjectEntity<?>> getPropertyExtras() {
        if(!finalizedPropertyExtras) {
            finalizedPropertyExtras = true;
            propertyExtras = ((MMap<PropertyDrawExtraType, PropertyObjectEntity<?>>)propertyExtras).immutable();
        }
        return (ImMap<PropertyDrawExtraType, PropertyObjectEntity<?>>) propertyExtras;
    }
    public void setPropertyExtra(PropertyObjectEntity<?> property, PropertyDrawExtraType type) {
        assert !finalizedPropertyExtras;
        if(property != null)
            ((MMap<PropertyDrawExtraType, PropertyObjectEntity<?>>) propertyExtras).add(type, property);
    }

    public boolean hasDynamicImage;

    public Group group;
    
    public Group getGroup() {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getParent());
    }
    public Group getNFGroup(Version version) {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getNFParent(version));
    }

    public boolean attr;

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
        public Type getType() {
            return getPropertyObjectEntity().getType();
        }

        @Override
        public String getReportSID() {
            return PropertyDrawEntity.this.getReportSID() + getReportSuffix();
        }

        @Override
        public PropertyObjectEntity getPropertyObjectEntity() {
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
            return getPropertyObjectEntity();
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

    public PropertyDrawEntity(int ID, String sID, ActionOrPropertyObjectEntity<P, ?> propertyObject, ActionOrProperty inheritedProperty) {
        super(ID);
        setSID(sID);
        setIntegrationSID(sID);
        this.propertyObject = propertyObject;
        this.inheritedProperty = inheritedProperty;
    }

    public DataClass getRequestInputType(FormEntity form, SecurityPolicy policy) {
        return getRequestInputType(CHANGE, form, policy, optimisticAsync);
    }

    public DataClass getWYSRequestInputType(FormEntity form, SecurityPolicy policy) {
        return getRequestInputType(CHANGE_WYS, form, policy, true); // wys is optimistic by default
    }
    
    public boolean isProperty() {
        return getValueActionOrProperty() instanceof PropertyObjectEntity;
    }

    public OrderEntity<?> getOrder() {
        return getValueProperty();
    }

    public DataClass getRequestInputType(String actionSID, FormEntity form, SecurityPolicy securityPolicy, boolean optimistic) {
        if (isProperty()) { // optimization
            ActionObjectEntity<?> changeAction = getEventAction(actionSID, form, securityPolicy);

            if (changeAction != null) {
                return (DataClass)changeAction.property.getSimpleRequestInputType(optimistic);
            }
        }
        return null;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form, SecurityPolicy policy) {
        ActionObjectEntity<A> changeAction = (ActionObjectEntity<A>) getEventAction(CHANGE, form, policy);
        if(changeAction!=null)
            return changeAction.getAddRemove(form);
        return null;
    }

    public <A extends PropertyInterface> String getOpenForm(FormEntity form, SecurityPolicy policy) {
        ActionObjectEntity<A> changeAction = (ActionObjectEntity<A>) getEventAction(CHANGE, form, policy);
        if(changeAction!=null)
            return changeAction.getOpenForm();
        return null;
    }

    public <A extends PropertyInterface> ModalityType getModalityType(FormEntity form, SecurityPolicy policy) {
        ActionObjectEntity<A> changeAction = (ActionObjectEntity<A>) getEventAction(CHANGE, form, policy);
        if(changeAction!=null)
            return changeAction.getModalityType();
        return null;
    }

    private boolean isChange(String eventActionSID) {
        // GROUP_CHANGE can also be in context menu binding (see Property constructor)
        boolean isEdit = CHANGE.equals(eventActionSID) || CHANGE_WYS.equals(eventActionSID) || GROUP_CHANGE.equals(eventActionSID);
        assert isEdit || hasContextMenuBinding(eventActionSID) || hasKeyBinding(eventActionSID);
        return isEdit;
    }
    
    private boolean checkPermission(Action eventAction, String eventActionSID, SQLCallable<Boolean> checkReadOnly, SecurityPolicy securityPolicy) throws SQLException, SQLHandledException {
        if(EDIT_OBJECT.equals(eventActionSID))
            return securityPolicy.checkPropertyEditObjectsPermission(getSecurityProperty());

        ActionOrProperty securityProperty;
        if (isChange(eventActionSID)) {
            if (isReadOnly() || (checkReadOnly != null && checkReadOnly.call())) 
                return false;

            securityProperty = getSecurityProperty(); // will check property itself
        } else { // menu or key binding
            securityProperty = eventAction;
        }

        return securityPolicy.checkPropertyChangePermission(securityProperty, eventAction);
    }

    public ActionObjectEntity<?> getEventAction(String actionId, FormEntity form, SecurityPolicy securityPolicy) {
        try {
            return getEventAction(actionId, form, null, securityPolicy);
        } catch (SQLException | SQLHandledException e) {
            assert false;
            throw Throwables.propagate(e);
        }
    }
    
    public ActionObjectEntity<?> getEventAction(String actionId, FormEntity form, SQLCallable<Boolean> checkReadOnly, SecurityPolicy securityPolicy) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> eventAction = getEventAction(actionId, form);

        if (eventAction != null && !checkPermission(eventAction.property, actionId, checkReadOnly, securityPolicy))
            return null;
        
        return eventAction;
    }

    public ActionObjectEntity<?> getEventAction(String actionId, FormEntity form) {
        if (eventActions != null) {
            ActionObjectEntity eventAction = eventActions.get(actionId);
            if (eventAction != null)
                return eventAction;
        }

        ActionOrProperty<P> eventProperty = getEventProperty();
        ActionMapImplement<?, P> eventActionImplement = eventProperty.getEventAction(actionId);
        if(eventActionImplement != null)
            return eventActionImplement.mapObjects(getEditMapping());

        // default implementations for group change and change wys
        if (GROUP_CHANGE.equals(actionId) || CHANGE_WYS.equals(actionId)) {
            ActionObjectEntity<?> eventAction = getEventAction(CHANGE, form);
            if (eventAction != null) {
                if (GROUP_CHANGE.equals(actionId)) // if there is no group change, then generate one
                    return eventAction.getGroupChange(getToDraw(form));
                else { // if CHANGE action requests DataClass, then use this action
                    assert CHANGE_WYS.equals(actionId);
                    if (eventAction.property.getSimpleRequestInputType(true) != null) // wys is optimistic by default
                        return eventAction;
                    else {
                        ActionMapImplement<?, P> defaultWYSAction = eventProperty.getDefaultWYSAction();
                        if(defaultWYSAction != null) // assert getSimpleRequestInputType != null
                            return defaultWYSAction.mapObjects(getEditMapping());
                    }
                }
            }
        }
        return null;
    }

    public ActionObjectEntity<?> getSelectorAction(FormEntity entity, Version version) {
        GroupObjectEntity groupObject = getNFToDraw(entity, version);
        if(groupObject != null) {
            for (ObjectEntity objectInstance : getObjectInstances().filter(groupObject.getObjects())) {
                if (objectInstance.baseClass instanceof CustomClass) {
                    ExplicitAction dialogAction = objectInstance.getChangeAction();
                    return new ActionObjectEntity<>(dialogAction, MapFact.singletonRev(dialogAction.interfaces.single(), objectInstance));
                }
            }
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
        contextMenuBindings.put(actionSID, caption);
    }

    public void setEventAction(String actionSID, ActionObjectEntity<?> eventAction) {
        if(eventActions ==null) {
            eventActions = new HashMap<>();
        }
        eventActions.put(actionSID, eventAction);
    }

    private ActionOrProperty<P> getEventProperty() {
        return propertyObject.property;
    }     
    private ImRevMap<P, ObjectEntity> getEditMapping() {
        return propertyObject.mapping;
    }     
    
    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        ImOrderMap<String, LocalizedString> propertyContextMenuBindings = getEventProperty().getContextMenuBindings(); 
        if (propertyContextMenuBindings.isEmpty()) {
            return contextMenuBindings;
        }

        OrderedMap<String, LocalizedString> result = new OrderedMap<>();
        for (int i = 0; i < propertyContextMenuBindings.size(); ++i) {
            result.put(propertyContextMenuBindings.getKey(i), propertyContextMenuBindings.getValue(i));
        }

        if (contextMenuBindings != null) {
            result.putAll(contextMenuBindings);
        }

        return result;
    }

    public boolean hasContextMenuBinding(String actionSid) {
        OrderedMap contextMenuBindings = getContextMenuBindings();
        return contextMenuBindings != null && contextMenuBindings.containsKey(actionSid);
    }
    
    public boolean hasKeyBinding(String actionId) {
        ImMap<KeyStroke, String> keyBindings = getKeyBindings();
        return keyBindings != null && keyBindings.containsValue(actionId);
    }

    public ImMap<KeyStroke, String> getKeyBindings() {
        ImMap<KeyStroke, String> propertyKeyBindings = getEventProperty().getKeyBindings();
        if(keyBindings != null)
            propertyKeyBindings = propertyKeyBindings.merge(MapFact.fromJavaMap(keyBindings), MapFact.override());
        return propertyKeyBindings;
    }

    public String getMouseBinding() {
        return mouseBinding != null ? mouseBinding : getEventProperty().getMouseBinding();
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

    public void proceedDefaultDraw(FormEntity form) {
        getInheritedProperty().drawOptions.proceedDefaultDraw(this, form);
    }

    @Override
    public String toString() {
        return (formPath == null ? "" : formPath) + " property:" + propertyObject.toString();
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
        return form.getNFApplyObject(getObjectInstances(), excludeGroupObjects, version);
    }

    @IdentityStartLazy
    public ImSet<ObjectEntity> getObjectInstances() { 
        MAddSet<ActionOrPropertyObjectEntity<?, ?>> propertyObjects = SetFact.mAddSet();

        PropertyDrawExtraType[] neededTypes = {CAPTION, FOOTER, SHOWIF, BACKGROUND, FOREGROUND, IMAGE, READONLYIF};
        for (PropertyDrawExtraType type : neededTypes) {
            PropertyObjectEntity<?> prop = getPropertyExtra(type);
            if (prop != null) {
                propertyObjects.add(prop);
            }
        }
        
        MSet<ObjectEntity> mObjects = SetFact.mSet();
        for(int i=0,size=propertyObjects.size();i<size;i++)
            mObjects.addAll(propertyObjects.get(i).getObjectInstances());
        mObjects.addAll(getValueActionOrProperty().getObjectInstances());
        if(toDraw != null)
            mObjects.addAll(toDraw.getObjects());
        return mObjects.immutable();
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

    public static <P extends PropertyInterface> String createSID(ActionOrPropertyObjectEntity<?, ?> property, ImOrderSet<P> interfaces) {
        assert property.property.isNamed();
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces)
            mapping.add(property.mapping.getObject(pi).getSID());
        return createSID(property.property.getName(), mapping);
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

    public Type getType() {
        return getValueProperty().property.getType();
    }

    public LocalizedString getCaption() {
        return getInheritedProperty().caption;
    }

    public boolean isNotNull() {
        return getInheritedProperty().isNotNull();
    }

    public String integrationSID; // hack - can be null for EXPORT FROM orders

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID;
    }
    
    public PropertyObjectEntity getImportProperty() {
        return (PropertyObjectEntity) propertyObject;
    }

    public PropertyObjectEntity<?> getDrawProperty() {
        return propertyObject.getDrawProperty(getPropertyExtras().get(READONLYIF));
    }

    // for getExpr, getType purposes
    public ActionOrPropertyObjectEntity<?, ?> getValueActionOrProperty() {
        return propertyObject;
    }

    public PropertyObjectEntity<?> getValueProperty() {
        return (PropertyObjectEntity) getValueActionOrProperty();
    }

    @Override
    public PropertyObjectEntity getPropertyObjectEntity() {
        return getValueProperty();
    }

    // presentation info, probably should be merged with inheritDrawOptions mechanism
    public ActionOrProperty getInheritedProperty() {
        return inheritedProperty;
    }

    public ActionOrProperty getSecurityProperty() {
        return getInheritedProperty();
    }

    // for debug purposes
    public ActionOrProperty getDebugBindingProperty() {
        return getInheritedProperty();
    }
    public ActionOrPropertyObjectEntity getDebugProperty() {
        return propertyObject;
    }

    @Override
    public String getReportSID() {
        return getSID();
    }
}
