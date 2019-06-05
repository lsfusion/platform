package lsfusion.server.logics.form.struct.property;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.print.ReportConstants;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityStartLazy;
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
import lsfusion.server.physics.admin.authentication.security.policy.ViewPropertySecurityPolicy;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.action.ServerResponse.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;
    
    private final ActionOrPropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<String, ActionObjectEntity<?>> editActions;

    public boolean optimisticAsync;

    public Boolean askConfirm;
    public String askConfirmMessage;

    public Boolean shouldBeLast;
    public ClassViewType forceViewType;
    public String eventID;

    private String formPath;
    
    public LocalizedString initCaption = null; // чисто техническая особенность реализации
    
    // предполагается что propertyObject ссылается на все (хотя и не обязательно)
    public String columnsName;
    public Object columnGroupObjects = SetFact.mOrderExclSet();
    private boolean finalizedColumnGroupObjects;

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public PropertyObjectEntity<?> propertyCaption;
    public PropertyObjectEntity<?> propertyShowIf;
    public PropertyObjectEntity<?> propertyReadOnly;
    public PropertyObjectEntity<?> propertyFooter;
    public PropertyObjectEntity<?> propertyBackground;
    public PropertyObjectEntity<?> propertyForeground;

    public ObjectEntity applyObject; // virtual object to change apply object (now used only EXPORT FROM plain formats)

    public Group group;
    
    public Group getGroup() {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getParent());
    }
    public Group getNFGroup(Version version) {
        return (group != null ? (group == Group.NOGROUP ? null : group) : getInheritedProperty().getNFParent(version));
    }

    public boolean attr;

    public PropertyDrawEntity quickFilterProperty;

    public void fillQueryProps(MExclSet<PropertyReaderEntity> mResult) {
        mResult.exclAdd(this);

        if (propertyCaption != null)
            mResult.exclAdd(captionReader);

        if (propertyFooter != null)
            mResult.exclAdd(footerReader);

        if (propertyShowIf != null)
            mResult.exclAdd(showIfReader);
    }

    private abstract class PropertyDrawReader implements PropertyReaderEntity {

        public Type getType() {
            return getPropertyObjectEntity().getType();
        }

        protected abstract String getReportSuffix();

        public String getReportSID() {
            return PropertyDrawEntity.this.getReportSID() + getReportSuffix();
        }

        @Override
        public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
            return PropertyDrawEntity.this.getColumnGroupObjects();
        }
    }

    public final PropertyReaderEntity captionReader = new PropertyDrawReader() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.CAPTION;
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
            return PropertyDrawEntity.this.propertyCaption;
        }

        @Override
        public Type getType() {
            return PropertyDrawEntity.this.propertyCaption.getType();
        }

        @Override
        public PropertyObjectEntity getPropertyObjectEntity() {
            return PropertyDrawEntity.this.propertyCaption;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.caption}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }

        @Override
        protected String getReportSuffix() {
            return ReportConstants.headerSuffix;
        }
    };
    
    
    public final PropertyReaderEntity footerReader = new PropertyDrawReader() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.FOOTER;
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
            return PropertyDrawEntity.this.propertyFooter;
        }

        @Override
        public PropertyObjectEntity getPropertyObjectEntity() {
            return PropertyDrawEntity.this.propertyFooter;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.footer}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }

        @Override
        protected String getReportSuffix() {
            return ReportConstants.footerSuffix;
        }
    };


    public final PropertyReaderEntity showIfReader = new PropertyDrawReader() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.SHOWIF;
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
            return PropertyDrawEntity.this.propertyShowIf;
        }

        @Override
        public PropertyObjectEntity getPropertyObjectEntity() {
            return PropertyDrawEntity.this.propertyShowIf;
        }

        @Override
        public String toString() {
            return "SHOWIF" + "(" + PropertyDrawEntity.this.toString() + ")";
        }

        @Override
        protected String getReportSuffix() {
            return ReportConstants.showIfSuffix;
        }
    };
    
    private ActionOrProperty inheritedProperty;

    public PropertyDrawEntity(int ID, ActionOrPropertyObjectEntity<P, ?> propertyObject, ActionOrProperty inheritedProperty) {
        super(ID);
        setSID("propertyDraw" + ID);
        setIntegrationSID("propertyDraw" + ID);
        this.propertyObject = propertyObject;
        this.inheritedProperty = inheritedProperty;
    }

    public DataClass getRequestInputType(SecurityPolicy policy) {
        return getRequestInputType(CHANGE, policy, optimisticAsync);
    }

    public DataClass getWYSRequestInputType(SecurityPolicy policy) {
        return getRequestInputType(CHANGE_WYS, policy, true); // wys is optimistic by default
    }
    
    public boolean isProperty() {
        return getValueActionOrProperty() instanceof PropertyObjectEntity;
    }

    public OrderEntity<?> getOrder() {
        return getValueProperty();
    }

    public DataClass getRequestInputType(String actionSID, SecurityPolicy policy, boolean optimistic) {
        if (isProperty()) { // optimization
            ActionObjectEntity<?> changeAction = getEditAction(actionSID, policy);

            if (changeAction != null) {
                return (DataClass)changeAction.property.getSimpleRequestInputType(optimistic);
            }
        }
        return null;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form, SecurityPolicy policy) {
        ActionObjectEntity<A> changeAction = (ActionObjectEntity<A>) getEditAction(CHANGE, policy);
        if(changeAction!=null)
            return changeAction.getAddRemove(form);
        return null;
    }

    private boolean isEdit(String editActionSID) {
        // GROUP_CHANGE can also be in context menu binding (see Property constructor)
        boolean isEdit = CHANGE.equals(editActionSID) || CHANGE_WYS.equals(editActionSID) || EDIT_OBJECT.equals(editActionSID) || GROUP_CHANGE.equals(editActionSID);
        assert isEdit || hasContextMenuBinding(editActionSID) || hasKeyBinding(editActionSID);
        return isEdit;
    }
    
    private boolean checkPermission(Action editAction, String editActionSID, SQLCallable<Boolean> checkReadOnly, ImSet<SecurityPolicy> securityPolicies) throws SQLException, SQLHandledException {
        ActionOrProperty securityProperty;
        if (isEdit(editActionSID) && !editAction.ignoreReadOnlyPolicy()) { // if event handler doesn't change anything (for example SELECTOR), consider this event to be binding (not edit) 
            if (isReadOnly() || (checkReadOnly != null && checkReadOnly.call())) 
                return false;

            securityProperty = getSecurityProperty(); // will check property itself 
        } else { // menu or key binding
            securityProperty = editAction;
        }

        for(SecurityPolicy securityPolicy : securityPolicies)
            if(!securityPolicy.property.change.checkPermission(securityProperty))
                return false;
        return true;
    }

    public ActionObjectEntity<?> getEditAction(String actionId, SecurityPolicy securityPolicy) {
        try {
            return getEditAction(actionId, null, securityPolicy != null ? SetFact.singleton(securityPolicy) : SetFact.<SecurityPolicy>EMPTY());
        } catch (SQLException | SQLHandledException e) {
            assert false;
            throw Throwables.propagate(e);
        }
    }
    
    public ActionObjectEntity<?> getEditAction(String actionId, SQLCallable<Boolean> checkReadOnly, ImSet<SecurityPolicy> securityPolicies) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> editAction = getEditAction(actionId);

        if (editAction != null && !checkPermission(editAction.property, actionId, checkReadOnly, securityPolicies))
            return null;
        
        return editAction;
    }

    public ActionObjectEntity<?> getEditAction(String actionId) {
        if (editActions != null) {
            ActionObjectEntity editAction = editActions.get(actionId);
            if (editAction != null)
                return editAction;
        }
        
        ActionOrProperty<P> editProperty = getEditProperty();
        ActionMapImplement<?, P> editActionImplement = editProperty.getEditAction(actionId);
        if(editActionImplement != null)
            return editActionImplement.mapObjects(getEditMapping());

        // default implementations for group change and change wys
        if (GROUP_CHANGE.equals(actionId) || CHANGE_WYS.equals(actionId)) {
            ActionObjectEntity<?> editAction = getEditAction(CHANGE);
            if (editAction != null) {
                if (GROUP_CHANGE.equals(actionId)) // if there is no group change, then generate one
                    return editAction.getGroupChange();
                else { // if CHANGE action requests DataClass, then use this action
                    assert CHANGE_WYS.equals(actionId);
                    if (editAction.property.getSimpleRequestInputType(true) != null) // wys is optimistic by default
                        return editAction;
                    else {
                        ActionMapImplement<?, P> defaultWYSAction = editProperty.getDefaultWYSAction();
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

    public void setEditAction(String actionSID, ActionObjectEntity<?> editAction) {
        if(editActions==null) {
            editActions = new HashMap<>();
        }
        editActions.put(actionSID, editAction);
    }

    private ActionOrProperty<P> getEditProperty() {
        return propertyObject.property;
    }     
    private ImRevMap<P, ObjectEntity> getEditMapping() {
        return propertyObject.mapping;
    }     

    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        ImOrderMap<String, LocalizedString> propertyContextMenuBindings = getEditProperty().getContextMenuBindings(); 
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
        Map keyBindings = getKeyBindings();
        return keyBindings != null && keyBindings.containsValue(actionId);
    }

    public Map<KeyStroke, String> getKeyBindings() {
        ImMap<KeyStroke, String> propertyKeyBindings = getEditProperty().getKeyBindings();
        if (propertyKeyBindings.isEmpty()) {
            return keyBindings;
        }

        Map<KeyStroke, String> result = propertyKeyBindings.toJavaMap();
        if (keyBindings != null) {
            result.putAll(keyBindings);
        }
        return result;
    }

    public String getMouseBinding() {
        return mouseBinding != null ? mouseBinding : getEditProperty().getMouseBinding();
    }

    @LongMutable
    public ImOrderSet<GroupObjectEntity> getColumnGroupObjects() {
        if(!finalizedColumnGroupObjects) {
            finalizedColumnGroupObjects = true;
            columnGroupObjects = ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).immutableOrder();
        }

        return (ImOrderSet<GroupObjectEntity>)columnGroupObjects;
    }
    public void setColumnGroupObjects(String columnsName, ImOrderSet<GroupObjectEntity> columnGroupObjects) {
        assert !finalizedColumnGroupObjects;
        this.columnsName = columnsName;
        finalizedColumnGroupObjects = true;
        this.columnGroupObjects = columnGroupObjects;
    }

    public void addColumnGroupObject(GroupObjectEntity columnGroupObject) {
        assert !finalizedColumnGroupObjects;
        ((MOrderExclSet<GroupObjectEntity>)columnGroupObjects).exclAdd(columnGroupObject);
    }

    public void setPropertyCaption(PropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption;
    }

    public void setPropertyCaptionAndShowIf(PropertyObjectEntity propertyCaptionAsShowIf) {
        this.propertyCaption = propertyCaptionAsShowIf;
        this.propertyShowIf = propertyCaptionAsShowIf;
    }

    public void setPropertyFooter(PropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyBackground(PropertyObjectEntity propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(PropertyObjectEntity propertyForeground) {
        this.propertyForeground = propertyForeground;
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
        return toDraw==null? getApplyObject(form, SetFact.<GroupObjectEntity>EMPTY(), true) :toDraw;
    }

    public GroupObjectEntity getApplyObject(FormEntity form, ImSet<GroupObjectEntity> excludeGroupObjects, boolean supportGroupColumns) {
        if(supportGroupColumns)
            excludeGroupObjects = excludeGroupObjects.merge(getColumnGroupObjects().getSet());
        return form.getApplyObject(getObjectInstances(), excludeGroupObjects);
    }

    @IdentityStartLazy
    public ImSet<ObjectEntity> getObjectInstances() {
        MAddSet<ActionOrPropertyObjectEntity<?, ?>> propertyObjects = SetFact.mAddSet();
        if(propertyCaption != null)
            propertyObjects.add(propertyCaption);
        if(propertyFooter != null)
            propertyObjects.add(propertyFooter);
        if(propertyShowIf != null)
            propertyObjects.add(propertyShowIf);
        MSet<ObjectEntity> mObjects = SetFact.mSet();
        for(int i=0,size=propertyObjects.size();i<size;i++)
            mObjects.addAll(propertyObjects.get(i).getObjectInstances());
        mObjects.addAll(getValueActionOrProperty().getObjectInstances());
        if(toDraw != null)
            mObjects.add(toDraw.getOrderObjects().get(0));
        return mObjects.immutable();
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        return toDraw==null?form.getNFApplyObject(getObjectInstances(), version):toDraw;
    }

    public boolean isToolbar(FormEntity formEntity) {
        if (forceViewType != null)
            return forceViewType.isToolbar();

        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.initClassView.isToolbar();
    }

    public boolean isGrid(FormEntity formEntity) {
        GroupObjectEntity toDraw = getToDraw(formEntity);
        return toDraw != null && toDraw.initClassView.isGrid() && (forceViewType == null || forceViewType.isGrid());        
    }

    public boolean isForcedPanel() {
        return forceViewType != null && forceViewType.isPanel();
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

    @Override
    public Object getProfiledObject() {
        return this;
    }

    @Override
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

    public boolean checkPermission(ViewPropertySecurityPolicy policy) {
        return policy.checkPermission(getSecurityProperty());
    }
    public void deny(ViewPropertySecurityPolicy policy) {
        policy.deny(getSecurityProperty());
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
