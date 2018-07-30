package lsfusion.server.form.entity;

import com.google.common.base.Throwables;
import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.PropertyReadType;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.NumericClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.*;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.ExplicitActionProperty;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.interop.form.ServerResponse.*;

public class PropertyDrawEntity<P extends PropertyInterface> extends IdentityObject implements Instantiable<PropertyDrawInstance>, PropertyReaderEntity {

    private PropertyEditType editType = PropertyEditType.EDITABLE;

    public PropertyObjectEntity<P, ?> propertyObject;
    
    public GroupObjectEntity toDraw;

    private String mouseBinding;
    private Map<KeyStroke, String> keyBindings;
    private OrderedMap<String, LocalizedString> contextMenuBindings;
    private Map<String, ActionPropertyObjectEntity<?>> editActions;

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
    public CalcPropertyObjectEntity<?> propertyCaption;
    public CalcPropertyObjectEntity<?> propertyShowIf;
    public CalcPropertyObjectEntity<?> propertyReadOnly;
    public CalcPropertyObjectEntity<?> propertyFooter;
    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    public PropertyDrawEntity quickFilterProperty;

    public final PropertyReaderEntity captionReader = new PropertyReaderEntity() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.CAPTION;
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public PropertyType getPropertyType(FormEntity formEntity) {
            return null;
        }

        @Override
        public Object getProfiledObject() {
            return PropertyDrawEntity.this.propertyCaption;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.caption}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }
    };
    
    
    public final PropertyReaderEntity footerReader = new PropertyReaderEntity() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.FOOTER;
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public PropertyType getPropertyType(FormEntity formEntity) {
            return null;
        }

        @Override
        public Object getProfiledObject() {
            return PropertyDrawEntity.this.propertyFooter;
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.footer}") + "(" + PropertyDrawEntity.this.toString() + ")";
        }
    };


    public final PropertyReaderEntity showIfReader = new PropertyReaderEntity() {
        @Override
        public byte getTypeID() {
            return PropertyReadType.SHOWIF;
        }

        @Override
        public int getID() {
            return PropertyDrawEntity.this.getID();
        }

        @Override
        public PropertyType getPropertyType(FormEntity formEntity) {
            return null;
        }

        @Override
        public Object getProfiledObject() {
            return PropertyDrawEntity.this.propertyShowIf;
        }

        @Override
        public String toString() {
            return "SHOWIF" + "(" + PropertyDrawEntity.this.toString() + ")";
        }
    };
    
    public PropertyDrawEntity() {
    }

    public PropertyDrawEntity(int ID, PropertyObjectEntity<P, ?> propertyObject, GroupObjectEntity toDraw) {
        super(ID);
        setSID("propertyDraw" + ID);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
    }

    public DataClass getRequestInputType(SecurityPolicy policy) {
        return getRequestInputType(CHANGE, policy, optimisticAsync);
    }

    public DataClass getWYSRequestInputType(SecurityPolicy policy) {
        return getRequestInputType(CHANGE_WYS, policy, false);
    }

    public DataClass getRequestInputType(String actionSID, SecurityPolicy policy, boolean optimistic) {
        if (propertyObject instanceof CalcPropertyObjectEntity) { // optimization
            ActionPropertyObjectEntity<?> changeAction = getEditAction(actionSID, policy);

            if (changeAction != null) {
                return (DataClass)changeAction.property.getSimpleRequestInputType(optimistic);
            }
        }
        return null;
    }

    public <A extends PropertyInterface> Pair<ObjectEntity, Boolean> getAddRemove(FormEntity form, SecurityPolicy policy) {
        ActionPropertyObjectEntity<A> changeAction = (ActionPropertyObjectEntity<A>) getEditAction(CHANGE, policy);
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
    
    private boolean checkPermission(ActionProperty editAction, String editActionSID, SQLCallable<Boolean> checkReadOnly, SecurityPolicy securityPolicy) throws SQLException, SQLHandledException {
        Property securityProperty;
        if (isEdit(editActionSID) && !editAction.ignoreReadOnlyPolicy()) { // if event handler doesn't change anything (for example SELECTOR), consider this event to be binding (not edit) 
            if (isReadOnly() || (checkReadOnly != null && checkReadOnly.call())) 
                return false;

            securityProperty = propertyObject.property; // will check property itself 
        } else { // menu or key binding
            securityProperty = editAction;
        }

        return securityPolicy == null || securityPolicy.property.change.checkPermission(securityProperty);
    }

    public ActionPropertyObjectEntity<?> getEditAction(String actionId, SecurityPolicy securityPolicy) {
        try {
            return getEditAction(actionId, securityPolicy, null);
        } catch (SQLException | SQLHandledException e) {
            assert false;
            throw Throwables.propagate(e);
        }
    }
    
    public ActionPropertyObjectEntity<?> getEditAction(String actionId, SecurityPolicy securityPolicy, SQLCallable<Boolean> checkReadOnly) throws SQLException, SQLHandledException {
        ActionPropertyObjectEntity<?> editAction = getEditAction(actionId);

        if (editAction != null && !checkPermission(editAction.property, actionId, checkReadOnly, securityPolicy))
            return null;
        
        return editAction;
    }

    public ActionPropertyObjectEntity<?> getEditAction(String actionId) {
        if (editActions != null) {
            ActionPropertyObjectEntity editAction = editActions.get(actionId);
            if (editAction != null)
                return editAction;
        }
        
        ActionPropertyMapImplement<?, P> editActionImplement = propertyObject.property.getEditAction(actionId);
        if(editActionImplement != null)
            return editActionImplement.mapObjects(propertyObject.mapping);

        // default implementations for group change and change wys
        if (GROUP_CHANGE.equals(actionId) || CHANGE_WYS.equals(actionId)) {
            ActionPropertyObjectEntity<?> editAction = getEditAction(CHANGE);
            if (editAction != null) {
                if (GROUP_CHANGE.equals(actionId)) // if there is no group change, then generate one
                    return editAction.getGroupChange();
                else {
                    assert CHANGE_WYS.equals(actionId);
                    if (editAction.property.getSimpleRequestInputType(optimisticAsync) != null) // if CHANGE action requests DataClass, then use this action 
                        return editAction;
                }
            }
        }
        return null;
    }

    public ActionPropertyObjectEntity<?> getSelectorAction(FormEntity entity, Version version) {
        Property<P> property = propertyObject.property;
        
        ImMap<P, ObjectEntity> groupObjects = propertyObject.mapping.filterValues(getNFToDraw(entity, version).getObjects()); // берем нижний объект в toDraw
        for (ObjectEntity objectInstance : groupObjects.valueIt()) {
            if (objectInstance.baseClass instanceof CustomClass) {
                ExplicitActionProperty dialogAction = objectInstance.getChangeAction(property);
                return new ActionPropertyObjectEntity<>(
                        dialogAction,
                        MapFact.singleton(dialogAction.interfaces.single(), objectInstance)
                );
            }
        }
        return null;
    }

    public void setPropertyObject(PropertyObjectEntity<P, ?> propertyObject) {
        this.propertyObject = propertyObject;
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

    public void setEditAction(String actionSID, ActionPropertyObjectEntity<?> editAction) {
        if(editActions==null) {
            editActions = new HashMap<>();
        }
        editActions.put(actionSID, editAction);
    }


    public OrderedMap<String, LocalizedString> getContextMenuBindings() {
        ImOrderMap<String, LocalizedString> propertyContextMenuBindings = propertyObject.property.getContextMenuBindings();
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
        ImMap<KeyStroke, String> propertyKeyBindings = propertyObject.property.getKeyBindings();
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
        return mouseBinding != null ? mouseBinding : propertyObject.property.getMouseBinding();
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

    public void setPropertyCaption(CalcPropertyObjectEntity propertyCaption) {
        this.propertyCaption = propertyCaption;
    }

    public void setPropertyCaptionAndShowIf(CalcPropertyObjectEntity propertyCaptionAsShowIf) {
        this.propertyCaption = propertyCaptionAsShowIf;
        this.propertyShowIf = propertyCaptionAsShowIf;
    }

    public void setPropertyFooter(CalcPropertyObjectEntity propertyFooter) {
        this.propertyFooter = propertyFooter;
    }

    public void setPropertyBackground(CalcPropertyObjectEntity propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(CalcPropertyObjectEntity propertyForeground) {
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
        propertyObject.property.drawOptions.proceedDefaultDesign(propertyView);
    }

    @Override
    public String toString() {
        return (formPath == null ? "" : formPath) + " property:" + propertyObject.toString();
    }

    public GroupObjectEntity getToDraw(FormEntity form) {
        return toDraw==null?form.getApplyObject(propertyObject.getObjectInstances()):toDraw;        
    }

    public GroupObjectEntity getNFToDraw(FormEntity form, Version version) {
        return toDraw==null?form.getNFApplyObject(propertyObject.getObjectInstances(), version):toDraw;
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

    public static <P extends PropertyInterface> String createSID(PropertyObjectEntity<?, ?> property, ImOrderSet<P> interfaces) {
        assert property.property.isNamed();
        List<String> mapping = new ArrayList<>();
        for (P pi : interfaces) {
            PropertyObjectInterfaceEntity obj = property.mapping.getObject(pi);
            assert obj instanceof ObjectEntity;
            mapping.add(((ObjectEntity) obj).getSID());
        }
        return createSID(property.property.getName(), mapping);
    }

    public String getFormPath() {
        return formPath;
    }

    public void setFormPath(String formPath) {
        this.formPath = formPath;
    }

    public String getNamespace() {
        return propertyObject == null || propertyObject.property == null ? "" : propertyObject.property.getNamespace();
    }

    public CalcPropertyObjectEntity getDrawInstance() {
        return propertyObject.getDrawProperty();
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
        return propertyObject.property.getType();
    }

    @Override
    public PropertyType getPropertyType(FormEntity formEntity) {
        Type type = getType();
        return new PropertyType(type.getSID(), getToDrawSID(formEntity), type.getCharLength().value, type instanceof NumericClass ? ((NumericClass) type).getPrecision() : 0);
    }

    private String getToDrawSID(FormEntity formEntity) {
        GroupObjectEntity group = getToDraw(formEntity);
        return group != null ? group.getSID() : "nogroup";
    }
}
