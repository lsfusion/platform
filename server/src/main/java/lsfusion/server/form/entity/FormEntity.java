package lsfusion.server.form.entity;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.serialization.CustomSerializable;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormEventType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.PropertyEditType;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.IdentityStrongLazy;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.*;
import lsfusion.server.logics.mutables.interfaces.*;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.NewSessionActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.serialization.ServerContext;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class FormEntity<T extends BusinessLogics<T>> extends NavigatorElement<T> implements ServerIdentitySerializable {
    private final static Logger logger = Logger.getLogger(FormEntity.class);

    public static final IsFullClientFormulaProperty isFullClient = IsFullClientFormulaProperty.instance;
    public static final IsDebugFormulaProperty isDebug = IsDebugFormulaProperty.instance;
    public static final SessionDataProperty isDialog = new SessionDataProperty("isDialog", "Is dialog", LogicalClass.instance);
    public static final SessionDataProperty isModal = new SessionDataProperty("isModal", "Is modal", LogicalClass.instance);
    public static final SessionDataProperty manageSession = new SessionDataProperty("manageSession", "Manage session", LogicalClass.instance);
    public static final SessionDataProperty isReadOnly = new SessionDataProperty("isReadOnly", "Is read only form", LogicalClass.instance);
    public static final SessionDataProperty showDrop = new SessionDataProperty("showDrop", "Show drop", LogicalClass.instance);

    public PropertyDrawEntity printActionPropertyDraw;
    public PropertyDrawEntity editActionPropertyDraw;
    public PropertyDrawEntity xlsActionPropertyDraw;
    public PropertyDrawEntity dropActionPropertyDraw;
    public PropertyDrawEntity refreshActionPropertyDraw;
    public PropertyDrawEntity applyActionPropertyDraw;
    public PropertyDrawEntity cancelActionPropertyDraw;
    public PropertyDrawEntity okActionPropertyDraw;
    public PropertyDrawEntity closeActionPropertyDraw;

    public NFMapList<Object, ActionPropertyObjectEntity<?>> eventActions = NFFact.mapList();
    public ImMap<Object, ImList<ActionPropertyObjectEntity<?>>> getEventActions() {
        return eventActions.getOrderMap();
    }
    public Iterable<ActionPropertyObjectEntity<?>> getEventActionsListIt(Object eventObject) {
        return eventActions.getListIt(eventObject);
    }

    private NFOrderSet<GroupObjectEntity> groups = NFFact.orderSet();
    public Iterable<GroupObjectEntity> getGroupsIt() {
        return groups.getIt();
    }
    public Iterable<GroupObjectEntity> getNFGroupsIt(Version version) { // не finalized
        return groups.getNFIt(version);
    }
    public ImOrderSet<GroupObjectEntity> getGroupsList() {
        return groups.getOrderSet(); 
    }
    public Iterable<GroupObjectEntity> getNFGroupsListIt(Version version) {
        return groups.getNFListIt(version);
    }
    
    private NFOrderSet<TreeGroupEntity> treeGroups = NFFact.orderSet();
    public Iterable<TreeGroupEntity> getTreeGroupsIt() {
        return treeGroups.getIt();
    }
    public ImList<TreeGroupEntity> getTreeGroupsList() {
        return treeGroups.getList(); 
    }
    public Iterable<TreeGroupEntity> getNFTreeGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return treeGroups.getNFListIt(version);
    }    
    
    private NFOrderSet<PropertyDrawEntity<?>> propertyDraws = NFFact.orderSet();
    public Iterable<PropertyDrawEntity<?>> getPropertyDrawsIt() {
        return propertyDraws.getIt();
    }
    public Iterable<PropertyDrawEntity<?>> getNFPropertyDrawsIt(Version version) {
        return propertyDraws.getNFIt(version);
    }
    public ImList<PropertyDrawEntity<?>> getPropertyDrawsList() {
        return propertyDraws.getList();        
    }
    public Iterable<PropertyDrawEntity<?>> getNFPropertyDrawsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return propertyDraws.getNFListIt(version);
    }
    
    private NFSet<FilterEntity> fixedFilters = NFFact.set();
    public ImSet<FilterEntity> getFixedFilters() {
        return fixedFilters.getSet();
    }
    
    private NFOrderSet<RegularFilterGroupEntity> regularFilterGroups = NFFact.orderSet();
    public Iterable<RegularFilterGroupEntity> getRegularFilterGroupsIt() {
        return regularFilterGroups.getIt();
    }
    public ImList<RegularFilterGroupEntity> getRegularFilterGroupsList() {
        return regularFilterGroups.getList();
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsIt(Version version) {
        return regularFilterGroups.getNFIt(version);        
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilterGroups.getNFListIt(version);
    }

    private NFOrderMap<PropertyDrawEntity<?>,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawEntity<?>,Boolean> getDefaultOrdersList() {
        return defaultOrders.getListMap();
    }
    public Boolean getNFDefaultOrder(PropertyDrawEntity<?> entity, Version version) {
        return defaultOrders.getNFValue(entity, version);
    }
    
    private NFOrderMap<OrderEntity<?>,Boolean> fixedOrders = NFFact.orderMap();
    public ImOrderMap<OrderEntity<?>,Boolean> getFixedOrdersList() {
        return fixedOrders.getListMap();
    }

    public String title;
    public ModalityType modalityType = ModalityType.DOCKED;
    public int autoRefresh = 0;
    public boolean keepSessionProperties = false;

    public boolean isSynchronizedApply = false;

    private PropertySIDPolicy policy;
    
    @SuppressWarnings("UnusedDeclaration")
    public FormEntity() {
    }
    
    public void finalizeInit(Version version) {
//        getNFRichDesign(version);
        richDesign.set(createDefaultRichDesign(version), version);
    }

    protected FormEntity(String sID, String caption, Version version) {
        this(null, sID, caption, null, null, version);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption, Version version) {
        this(parent, sID, caption, null, null, version);
        finalizeInit(version);
    }

    public FormEntity(String sID, String caption, String ititle, String icon, Version version) {
        this(null, sID, caption, ititle, icon, version);
    }
    
    private FormEntity(NavigatorElement<T> parent, String sID, String caption, String ititle, String icon, Version version) {
        super(parent, sID, caption, null, version);
        setImage(icon != null ? icon : "/images/form.png");
        logger.debug("Initializing form " + caption + "...");

        title = ititle;

        BaseLogicsModule baseLM = ThreadLocalContext.getBusinessLogics().LM;

        policy = baseLM.getSIDPolicy();

        printActionPropertyDraw = addPropertyDraw(baseLM.getFormPrint(), version);
        editActionPropertyDraw = addPropertyDraw(baseLM.getFormEdit(), version);
        xlsActionPropertyDraw = addPropertyDraw(baseLM.getFormXls(), version);
        refreshActionPropertyDraw = addPropertyDraw(baseLM.getFormRefresh(), version);
        applyActionPropertyDraw = addPropertyDraw(baseLM.getFormApply(), version);
        cancelActionPropertyDraw = addPropertyDraw(baseLM.getFormCancel(), version);
        okActionPropertyDraw = addPropertyDraw(baseLM.getFormOk(), version);
        closeActionPropertyDraw = addPropertyDraw(baseLM.getFormClose(), version);
        dropActionPropertyDraw = addPropertyDraw(baseLM.getFormDrop(), version);

        addActionsOnEvent(FormEventType.QUERYOK, true, version, (ActionPropertyObjectEntity)okActionPropertyDraw.propertyObject);
        addActionsOnEvent(FormEventType.QUERYCLOSE, true, version, (ActionPropertyObjectEntity)closeActionPropertyDraw.propertyObject);
    }

    public void addFixedFilter(FilterEntity filter, Version version) {
        fixedFilters.add(filter, version);
    }

    public void addFixedOrder(OrderEntity order, boolean descending, Version version) {
        fixedOrders.add(order, descending, version);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group, Version version) {
        regularFilterGroups.add(group, version);
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilterGroup(group, version);
    }
    
    public void addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, boolean isDefault, Version version) {
        filterGroup.addFilter(filter, isDefault, version);
        
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilter(filterGroup, filter, version);
    }

    // получает свойства, которые изменяют propChanges и соответственно hint'ить нельзя - временная затычка
    public ImSet<CalcProperty> getChangeModifierProps() {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            Property property = propertyDraw.propertyObject.property;
            if(property instanceof CalcProperty) {
                ImSet<CalcProperty> depends = ((CalcProperty<?>) property).getRecDepends();
                for(int i=0,size=depends.size();i<size;i++) {
                    CalcProperty depend = depends.get(i);
                    if(depend instanceof SumGroupProperty && ((SumGroupProperty)depend).distribute!=null)
                        mResult.add(((SumGroupProperty)depend).distribute.property);
                }
            }
        }
        return mResult.immutable();
    }

    // счетчик идентификаторов
    private IDGenerator idGenerator = new DefaultIDGenerator();

    public IDGenerator getIDGenerator() {
        return idGenerator;
    }

    public int genID() {
        return idGenerator.idShift();
    }

    public GroupObjectEntity getGroupObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getID() == id) {
                return group;
            }
        }

        return null;
    }

    public GroupObjectEntity getNFGroupObject(String sID, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public TreeGroupEntity getTreeGroup(int id) {
        for (TreeGroupEntity treeGroup : getTreeGroupsIt()) {
            if (treeGroup.getID() == id) {
                return treeGroup;
            }
        }

        return null;
    }

    public ObjectEntity getObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getObject(String sid) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(String sid, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(ValueClass cls, Version version) {
        for (GroupObjectEntity group : getNFGroupsListIt(version)) { // для детерменированности
            for (ObjectEntity object : group.getObjects()) {
                if (cls.equals(object.baseClass)) {
                    return object;
                }
            }
        }
        return null;
    }

    public List<String> getNFObjectsNamesAndClasses(List<ValueClass> classes, Version version) {
        List<String> names = new ArrayList<String>();
        classes.clear();
        
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                names.add(object.getSID());
                classes.add(object.baseClass);
            }
        }
        return names;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterGroupEntity getNFRegularFilterGroup(String sid, Version version) {
        if (sid == null) {
            return null;
        }

        for (RegularFilterGroupEntity filterGroup : getNFRegularFilterGroupsIt(version)) {
            if (sid.equals(filterGroup.getSID())) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterEntity getRegularFilter(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            for (RegularFilterEntity filter : filterGroup.getFiltersList()) {
                if (filter.getID() == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    @IdentityLazy
    public boolean isReadOnly() {
        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            if (!property.isReadOnly() && !(property.propertyObject.property instanceof NewSessionActionProperty)) {
                return false;
            }
        }

        return true;
    }

    public ObjectEntity addSingleGroupObject(ValueClass baseClass, String caption, Version version, Object... groups) {
        return addSingleGroupObject(genID(), (String) null, baseClass, caption, version, groups);
    }

    public ObjectEntity addSingleGroupObject(String sID, ValueClass baseClass, String caption, Version version, Object... groups) {
        return addSingleGroupObject(genID(), sID, baseClass, caption, version, groups);
    }

    public ObjectEntity addSingleGroupObject(int ID, String sID, ValueClass baseClass, String caption, Version version, Object... groups) {

        GroupObjectEntity groupObject = new GroupObjectEntity(ID, sID);
        ObjectEntity object = new ObjectEntity(ID, sID, baseClass, caption);
        groupObject.add(object);
        addGroupObject(groupObject, version);

        addPropertyDraw(groups, false, version, object);

        return object;
    }

    public ObjectEntity addSingleGroupObject(ValueClass baseClass, Version version, Object... groups) {
        return addSingleGroupObject(baseClass, null, version, groups);
    }

    public ObjectEntity addSingleGroupObject(int ID, ValueClass baseClass, Version version, Object... groups) {
        return addSingleGroupObject(ID, (String) null, baseClass, null, version, groups);
    }

    public ObjectEntity addSingleGroupObject(int ID, String sID, ValueClass baseClass, Version version, Object... groups) {
        return addSingleGroupObject(ID, sID, baseClass, null, version, groups);
    }

    public TreeGroupEntity addTreeGroupObject(String sID, Version version, GroupObjectEntity... tGroups) {
        TreeGroupEntity treeGroup = new TreeGroupEntity(genID());
        if (sID != null)
            treeGroup.setSID(sID);
        for (GroupObjectEntity group : tGroups) {
            if(!groups.containsNF(group, version))
                groups.add(group, version);
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addTreeGroup(treeGroup, version);

        return treeGroup;
    }

    public void addGroupObject(GroupObjectEntity group, Version version) {
        // регистрируем ID'шники, чтобы случайно не пересеклись заданные вручную и сгенерированные ID'шники
        idGenerator.idRegister(group.getID());
        for (ObjectEntity obj : group.getObjects()) {
            idGenerator.idRegister(obj.getID());
        }

        for (GroupObjectEntity groupOld : getNFGroupsIt(version)) {
            assert group.getID() != groupOld.getID() && !group.getSID().equals(groupOld.getSID());
            for (ObjectEntity obj : group.getObjects()) {
                for (ObjectEntity objOld : groupOld.getObjects()) {
                    assert obj.getID() != objOld.getID() && !obj.getSID().equals(objOld.getSID());
                }
            }
        }
        groups.add(group, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addGroupObject(group, version);
    }

    public void addPropertyDraw(ObjectEntity object, Version version, Object... groups) {
        addPropertyDraw(groups, false, version, object);
    }

    public void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, Version version, Object... groups) {
        addPropertyDraw(groups, false, version, object1, object2);
    }

    public void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, Version version, Object... groups) {
        addPropertyDraw(groups, false, version, object1, object2, object3);
    }

    public void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, Version version, Object... groups) {
        addPropertyDraw(groups, false, version, object1, object2, object3, object4);
    }

    private void addPropertyDraw(Object[] groups, boolean useObjSubsets, Version version, ObjectEntity... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) {
//                continue;
            } else if (group instanceof AbstractNode) {
                boolean upClasses = false;
                if ((i + 1) < groups.length && groups[i + 1] instanceof Boolean) {
                    upClasses = (Boolean) groups[i + 1];
                }
                addPropertyDraw((AbstractNode) group, upClasses, useObjSubsets, version, objects);
            } else if (group instanceof LP) {
                this.addPropertyDraw((LP) group, version, objects);
            } else if (group instanceof LP[]) {
                this.addPropertyDraw((LP[]) group, version, objects);
            }
        }
    }

    public List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, Version version, ObjectEntity... objects) {
        return addPropertyDraw(group, upClasses, null, false, version, objects);
    }

    public List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, Version version, ObjectEntity... objects) {
        return addPropertyDraw(group, false, null, false, version, objects);
    }

    public void addPropertyDraw(AbstractNode group, boolean upClasses, boolean useObjSubsets, Version version, ObjectEntity... objects) {
        addPropertyDraw(group, false, upClasses, null, useObjSubsets, version, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean prev, boolean upClasses, boolean useObjSubsets, Version version, ObjectEntity... objects) {
        addPropertyDraw(group, prev, upClasses, null, useObjSubsets, version, objects);
    }

    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, Version version, ObjectEntity... objects) {
        return addPropertyDraw(group, false, upClasses, groupObject, useObjSubsets, version, objects);
    }

    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean prev, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, Version version, ObjectEntity... objects) {
        ImOrderSet<ObjectEntity> orderObjects = SetFact.toOrderExclSet(objects);
        ImRevMap<ObjectEntity, ValueClassWrapper> objectToClass = orderObjects.getSet().mapRevValues(new GetValue<ValueClassWrapper, ObjectEntity>() {
            public ValueClassWrapper getMapValue(ObjectEntity value) {
                return new ValueClassWrapper(value.baseClass);
            }
        });
        ImSet<ValueClassWrapper> valueClasses = objectToClass.valuesSet();

        ImCol<ImSet<ValueClassWrapper>> classSubsets;
        if (useObjSubsets) {
            MCol<ImSet<ValueClassWrapper>> mClassSubsets = ListFact.mCol();
            for (ImSet<ValueClassWrapper> set : new Subsets<ValueClassWrapper>(valueClasses)) {
                if (!set.isEmpty()) {
                    mClassSubsets.add(set);
                }
            }
            classSubsets = mClassSubsets.immutableCol();
        } else {
            classSubsets = SetFact.singleton(valueClasses);
        }

        List<PropertyDrawEntity> propertyDraws = new ArrayList<PropertyDrawEntity>();

        ImOrderSet<ValueClassWrapper> orderInterfaces = orderObjects.mapOrder(objectToClass);
        for (PropertyClassImplement implement : group.getProperties(classSubsets, upClasses, version)) {
            ImSet<ValueClassWrapper> wrapers = implement.mapping.valuesSet();
            ImOrderSet<ObjectEntity> filterObjects = orderObjects.filterOrderIncl(objectToClass.filterValuesRev(wrapers).keys());
            propertyDraws.add(addPropertyDraw(implement.createLP(orderInterfaces.filterOrderIncl(wrapers), prev), groupObject, version, filterObjects.toArray(new ObjectEntity[filterObjects.size()])));
        }

        return propertyDraws;
    }

    public PropertyDrawEntity addPropertyDraw(LP property, Version version, PropertyObjectInterfaceEntity... objects) {
        return addPropertyDraw(property, null, version, objects);
    }

    public void addPropertyDraw(LP[] properties, Version version, ObjectEntity... objects) {
        /*
        Map<ValueClass, ObjectEntity> classToObject = new HashMap<ValueClass, ObjectEntity>();
        for (ObjectEntity object : objects) {
            Object oldValue = classToObject.put(object.baseClass, object);
            assert oldValue == null; // ValueClass объектов не должны совпадать
        }

        for (LP property : properties) {
            List<ObjectEntity> orderedObjects =
                    BaseUtils.mapList(property.listInterfaces, BaseUtils.join(property.property.getMapClasses(), classToObject));
            addPropertyDraw(property, null, orderedObjects.toArray(new ObjectEntity[1]));
        }
        */
        for (LP property : properties) {
            addPropertyDraw(property, version, objects);
        }
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P, ?> property, GroupObjectEntity groupObject, Version version, PropertyObjectInterfaceEntity... objects) {
        return addPropertyDraw(groupObject, property.createObjectEntity(objects), version);
    }

    public GroupObjectEntity getNFApplyObject(Collection<ObjectEntity> objects, Version version) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getNFGroupsListIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    public GroupObjectEntity getApplyObject(Collection<ObjectEntity> objects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getGroupsList()) {
            for (ObjectEntity object : group.getObjects()) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    public <I extends PropertyInterface, P extends Property<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImMap<I, ? extends PropertyObjectInterfaceEntity> mapping, Version version) {
        return addPropertyDraw(null, PropertyObjectEntity.create(property, mapping, null, null), version);
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P, ?> propertyImplement, Version version) {
        final PropertyDrawEntity<P> newPropertyDraw = new PropertyDrawEntity<P>(genID(), propertyImplement, groupObject);

        propertyImplement.property.proceedDefaultDraw(newPropertyDraw, this, version);

        if (propertyImplement.property.getSID() != null) {
            String propertySID = policy.createPropertyDrawSID(propertyImplement);  
            setPropertyDrawSID(newPropertyDraw, propertySID);
        }

        propertyDraws.add(newPropertyDraw, new FindIndex<PropertyDrawEntity<?>>() {
            public int getIndex(List<PropertyDrawEntity<?>> list) {
                int ind = list.size() - 1;
                if (!newPropertyDraw.shouldBeLast) {
                    while (ind >= 0) {
                        PropertyDrawEntity property = list.get(ind);
                        if (!property.shouldBeLast) {
                            break;
                        }
                        --ind;
                    }
                }
                return ind + 1;
            }
        }, version);
        return newPropertyDraw;
    }

    public void addPropertyDrawView(PropertyDrawEntity propertyDraw, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.addPropertyDraw(propertyDraw, version);
        }
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour, Version version) {
        propertyDraws.move(property, newNeighbour, isRightNeighbour, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.movePropertyDrawTo(property, newNeighbour, isRightNeighbour, version);
        }
    }

    public void setPropertyDrawSID(PropertyDrawEntity property, String sid) {
        property.setSID(null);
//        assert getPropertyDraw(sid) == null; 
        property.setSID(sid);
    }

    public PropertyObjectEntity addPropertyObject(LP property, PropertyObjectInterfaceEntity... objects) {
        if (property instanceof LCP) {
            return addPropertyObject((LCP<?>) property, objects);
        } else {
            return addPropertyObject((LAP<?>) property, objects);
        }
    }
    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(LCP<P> property, PropertyObjectInterfaceEntity... objects) {
        return addPropertyObject(property, property.getMap(objects));
    }
    public <P extends PropertyInterface> ActionPropertyObjectEntity<P> addPropertyObject(LAP<P> property, PropertyObjectInterfaceEntity... objects) {
        return addPropertyObject(property, property.getMap(objects));
    }

    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(LP property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        if (property instanceof LCP) {
            return addPropertyObject((LCP) property, objects);
        } else {
            return addPropertyObject((LAP) property, objects);
        }
    }
    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(LCP<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        return new CalcPropertyObjectEntity<P>(property.property, objects, property.getCreationScript(), property.getCreationPath());
    }
    public <P extends PropertyInterface> ActionPropertyObjectEntity<P> addPropertyObject(LAP<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        return new ActionPropertyObjectEntity<P>(property.property, objects, property.getCreationScript(), property.getCreationPath());
    }

    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(Property<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        if (property instanceof CalcProperty) {
            return addPropertyObject((CalcProperty) property, objects);
        } else {
            return addPropertyObject((ActionProperty) property, objects);
        }
    }
    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        return new CalcPropertyObjectEntity<P>(property, objects);
    }
    public <P extends PropertyInterface> ActionPropertyObjectEntity<P> addPropertyObject(ActionProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceEntity> objects) {
        return new ActionPropertyObjectEntity<P>(property, objects);
    }

    public PropertyDrawEntity<?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDraw(String sid, Version version) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getSID())) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDraw(String name, List<String> mapping, Version version) {
        return getPropertyDraw(PropertyDrawEntity.createSID(name, mapping), version);
    }

    public List<PropertyDrawEntity> getPropertyDrawList(LP...properties) {
        List<PropertyDrawEntity> list = new ArrayList<PropertyDrawEntity>();
        for (LP property : properties) {
            list.add(getPropertyDraw(property));
        }
        return list;
    }


    protected CalcPropertyObjectEntity getCalcPropertyObject(LCP<?> lp) {
        return (CalcPropertyObjectEntity) getPropertyDraw(lp).propertyObject;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?, ?> lp) {
        return getPropertyDraw(lp).propertyObject;
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?, ?> lp) {
        return getPropertyDraw(lp.property);
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?, ?> lp, int index) {
        return getPropertyDraw(lp.property, index);
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?, ?> lp, ObjectEntity object) {
        return getPropertyDraw(lp.property, object.groupTo);
    }

    protected PropertyDrawEntity getPropertyDraw(PropertyObjectEntity property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.propertyObject.equals(property)) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public PropertyObjectEntity getPropertyObject(Property property) {
        return getPropertyDraw(property).propertyObject;
    }

    protected PropertyDrawEntity getPropertyDraw(Property property) {
        return getPropertyDraw(property, 0);
    }

    protected PropertyDrawEntity getPropertyDraw(Property property, int index) {

        int cnt = 0;
        for (PropertyDrawEntity<?> propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.propertyObject.property == property) {
                if (cnt == index) {
                    return propertyDraw;
                } else {
                    cnt++;
                }
            }
        }

        return null;
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, ObjectEntity object) {
        return getPropertyDraw(group, object.groupTo);
    }

    public PropertyDrawEntity getNFPropertyDraw(AbstractNode group, ObjectEntity object, Version version) {
        return getNFPropertyDraw(group, object.groupTo, version);
    }

    /**
     * ищет PropertyDrawEntity, который мэпит на входы LP переданные objects
     */
    public PropertyDrawEntity getNFPropertyDraw(LP lp, Version version, PropertyObjectInterfaceEntity... objects) {
        if (lp.listInterfaces.size() != objects.length) {
            return null;
        }

        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            PropertyObjectEntity propertyObject = propertyDraw.propertyObject;
            if (propertyObject.property == lp.property) {
                boolean found = true;
                for (int i = 0; i < objects.length; ++i) {
                    Object iFace = lp.listInterfaces.get(i);
                    if (propertyObject.mapping.get(iFace) != objects[i]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return propertyDraw;
                }
            }
        }
        return null;
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, GroupObjectEntity groupObject) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (group.hasChild(propertyDraw.propertyObject.property) && groupObject.equals(propertyDraw.getToDraw(this))) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public PropertyDrawEntity getNFPropertyDraw(AbstractNode group, GroupObjectEntity groupObject, Version version) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (group.hasNFChild(propertyDraw.propertyObject.property, version) && groupObject.equals(propertyDraw.getNFToDraw(this, version))) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    private NFSet<CalcProperty> hintsIncrementTable = NFFact.set();
    @LongMutable
    public ImSet<CalcProperty> getHintsIncrementTable() {
        return hintsIncrementTable.getSet();
    }

    public void addHintsIncrementTable(Version version, LCP... props) {
        for (LP prop : props) {
            hintsIncrementTable.add((CalcProperty) prop.property, version);
        }
    }

    public void addHintsIncrementTable(Version version, CalcProperty... props) {
        for (CalcProperty prop : props) {
            hintsIncrementTable.add(prop, version);
        }
    }

    private NFSet<CalcProperty> hintsNoUpdate = NFFact.set();
    @LongMutable
    public ImSet<CalcProperty> getHintsNoUpdate() {
        return hintsNoUpdate.getSet();
    }

    @IdentityLazy
    public FunctionSet<CalcProperty> getNoHints() {
        if (Settings.get().isDisableChangeModifierAllHints())
            return BaseUtils.universal(getChangeModifierProps().isEmpty());
        else
            return CalcProperty.getDependsOnSet(getChangeModifierProps()); // тут какая то проблема есть
    }

    public void addHintsNoUpdate(Version version, LCP... props) {
        for (LCP prop : props) {
            addHintsNoUpdate(prop, version);
        }
    }

    protected void addHintsNoUpdate(LCP prop, Version version) {
        addHintsNoUpdate((CalcProperty) prop.property, version);
    }

    public void addHintsNoUpdate(CalcProperty prop, Version version) {
        hintsNoUpdate.add(prop, version);
    }

    public FormView createDefaultRichDesign(Version version) {
        return new DefaultFormView(this, version);
    }

    private NFProperty<FormView> richDesign = NFFact.property();

    public FormView getRichDesign() {
        return richDesign.get(); // assert что не null см. последнюю строку в конструкторе
/*        return richDesign.getDefault(new NFDefault<FormView>() {
            public FormView create() {
                return createDefaultRichDesign(Version.LAST);
            }
        });*/
    }

    public FormView getNFRichDesign(Version version) {
        return richDesign.getNF(version);
/*        FormView view = richDesign.getNF(version);
        if(view == null) {
            view = createDefaultRichDesign(version);
            richDesign.set(view, version);
        }
        return view;*/
    }

    public void setRichDesign(FormView view, Version version) {
        richDesign.set(view, version);
    }

    private GroupObjectHierarchy groupHierarchy;

    public GroupObjectHierarchy.ReportHierarchy getReportHierarchy() {
        return getGroupHierarchy().createReportHierarchy();
    }

    public GroupObjectHierarchy.ReportHierarchy getSingleGroupReportHierarchy(int groupId) {
        return getGroupHierarchy().createSingleGroupReportHierarchy(groupId);
    }

    public void modifyHierarchy(GroupObjectHierarchy groupHierarchy) {
    }

    public GroupObjectHierarchy getGroupHierarchy() {
        if (groupHierarchy == null) {
            FormGroupHierarchyCreator creator = new FormGroupHierarchyCreator(this);
            groupHierarchy = creator.createHierarchy();
            modifyHierarchy(groupHierarchy);
        }
        return groupHierarchy;
    }

    public byte getTypeID() {
        return 0;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        pool.writeString(outStream, title);
        pool.writeString(outStream, sID);
        outStream.writeUTF(modalityType.name());
        outStream.writeInt(autoRefresh);

        pool.serializeCollection(outStream, getGroupsList());
        pool.serializeCollection(outStream, getTreeGroupsList());
        pool.serializeCollection(outStream, getPropertyDrawsList());
        pool.serializeCollection(outStream, getFixedFilters());
        pool.serializeCollection(outStream, getRegularFilterGroupsList());

        pool.serializeObject(outStream, printActionPropertyDraw);
        pool.serializeObject(outStream, editActionPropertyDraw);
        pool.serializeObject(outStream, xlsActionPropertyDraw);
        pool.serializeObject(outStream, dropActionPropertyDraw);
        pool.serializeObject(outStream, refreshActionPropertyDraw);
        pool.serializeObject(outStream, applyActionPropertyDraw);
        pool.serializeObject(outStream, cancelActionPropertyDraw);
        pool.serializeObject(outStream, okActionPropertyDraw);
        pool.serializeObject(outStream, closeActionPropertyDraw);

        ImOrderMap<PropertyDrawEntity<?>, Boolean> defaultOrders = getDefaultOrdersList();
        outStream.writeInt(defaultOrders.size());
        for (int i=0,size=defaultOrders.size();i<size;i++) {
            pool.serializeObject(outStream, defaultOrders.getKey(i), serializationType);
            outStream.writeBoolean(defaultOrders.getValue(i));
        }

        ImMap<Object, ImList<ActionPropertyObjectEntity<?>>> eventActions = getEventActions();
        outStream.writeInt(eventActions.size());
        for (int i=0,size=eventActions.size();i<size;i++) {
            Object event = eventActions.getKey(i);

            if (event instanceof String) {
                outStream.writeByte(0);
                pool.writeString(outStream, (String) event);
            } else if (event instanceof CustomSerializable) {
                outStream.writeByte(1);
                pool.serializeObject(outStream, (CustomSerializable) event);
            } else {
                outStream.writeByte(2);
                pool.writeObject(outStream, event);
            }

            pool.serializeCollection(outStream, eventActions.getValue(i));
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
        title = pool.readString(inStream);
        sID = pool.readString(inStream);
        modalityType = ModalityType.valueOf(inStream.readUTF());
        autoRefresh = inStream.readInt();

        groups = NFFact.finalOrderSet(pool.<GroupObjectEntity>deserializeList(inStream));
        treeGroups = NFFact.finalOrderSet(pool.<TreeGroupEntity>deserializeList(inStream));
        propertyDraws = NFFact.finalOrderSet(pool.<PropertyDrawEntity<?>>deserializeList(inStream));
        fixedFilters = NFFact.finalSet(pool.<FilterEntity>deserializeSet(inStream));
        regularFilterGroups = NFFact.finalOrderSet(pool.<RegularFilterGroupEntity>deserializeList(inStream));

        printActionPropertyDraw = pool.deserializeObject(inStream);
        editActionPropertyDraw = pool.deserializeObject(inStream);
        xlsActionPropertyDraw = pool.deserializeObject(inStream);
        dropActionPropertyDraw = pool.deserializeObject(inStream);
        refreshActionPropertyDraw = pool.deserializeObject(inStream);
        applyActionPropertyDraw = pool.deserializeObject(inStream);
        cancelActionPropertyDraw = pool.deserializeObject(inStream);
        okActionPropertyDraw = pool.deserializeObject(inStream);
        closeActionPropertyDraw = pool.deserializeObject(inStream);

        int orderCount = inStream.readInt();
        MOrderExclMap<PropertyDrawEntity<?>, Boolean> mDefaultOrders = MapFact.mOrderExclMap(orderCount);
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawEntity order = pool.deserializeObject(inStream);
            mDefaultOrders.exclAdd(order, inStream.readBoolean());
        }
        defaultOrders = NFFact.finalOrderMap(mDefaultOrders.immutableOrder());

        int length = inStream.readInt();
        MExclMap<Object, ImList<ActionPropertyObjectEntity<?>>> mEventActions = MapFact.<Object, ImList<ActionPropertyObjectEntity<?>>>mExclMap(length);
        for (int i = 0; i < length; ++i) {
            Object event;
            switch (inStream.readByte()) {
                case 0 : event = pool.readString(inStream); break;
                case 1 : event = pool.deserializeObject(inStream); break;
                default : event = pool.readObject(inStream); break;
            }

            ImList<ActionPropertyObjectEntity<?>> actions = ListFact.fromJavaList(pool.<ActionPropertyObjectEntity<?>>deserializeList(inStream));
            mEventActions.exclAdd(event, actions);
        }
        eventActions = NFFact.finalMapList(mEventActions.immutable());
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeUTF(modalityType.name());
    }

    public void setAddOnTransaction(ObjectEntity entity, LogicsModule lm) {
        setAddOnEvent(entity, lm, FormEventType.INIT, FormEventType.APPLY, FormEventType.CANCEL);
    }

    public void setAddOnEvent(ObjectEntity entity, LogicsModule lm, FormEventType... events) {
        boolean needApplyConfirm = false;
        boolean needOkConfirm = false;
        for (FormEventType event : events) {
            if (event == FormEventType.APPLY) {
                needApplyConfirm = true;
            } else if (event == FormEventType.APPLY) {
                needOkConfirm = true;
            }
        }

        if (needOkConfirm) {
            okActionPropertyDraw.askConfirm = true;
            okActionPropertyDraw.askConfirmMessage = (okActionPropertyDraw.askConfirmMessage == null ? "" : okActionPropertyDraw.askConfirmMessage)
                    + getString("form.create.new.object") + " " + entity.getCaption() + " ?";
        }

        if (needApplyConfirm) {
            applyActionPropertyDraw.askConfirm = true;
            applyActionPropertyDraw.askConfirmMessage = (applyActionPropertyDraw.askConfirmMessage == null ? "" : applyActionPropertyDraw.askConfirmMessage)
                    + getString("form.create.new.object") + " " + entity.getCaption() + " ?";
        }

        addActionsOnEvent(addPropertyObject(lm.getAddObjectAction(this, entity)), lm.getVersion(), events);
    }

    public void addActionsOnObjectChange(ObjectEntity object, Version version, ActionPropertyObjectEntity... actions) {
        addActionsOnObjectChange(object, false, version, actions);
    }

    public void addActionsOnObjectChange(ObjectEntity object, boolean drop, Version version, ActionPropertyObjectEntity... actions) {
        addActionsOnEvent(object, drop, version, actions);
    }

    public void addActionsOnEvent(ActionPropertyObjectEntity action, Version version, Object... events) {
        for(Object event : events)
            addActionsOnEvent(event, version, action);
    }

    public void addActionsOnEvent(Object eventObject, Version version, ActionPropertyObjectEntity<?>... actions) {
        addActionsOnEvent(eventObject, false, version, actions);
    }

    public void addActionsOnEvent(Object eventObject, boolean drop, Version version, ActionPropertyObjectEntity<?>... actions) {
        if(drop)
            eventActions.removeAll(eventObject, version);
        eventActions.addAll(eventObject, Arrays.asList(actions), version);
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, byte[] formState) {
        return deserialize(BL, new DataInputStream(new ByteArrayInputStream(formState)));
    }

    public ComponentView getDrawTabContainer(PropertyDrawEntity<?> property, boolean grid) {
        FormView formView = getRichDesign();
        ComponentView drawComponent;
        if(grid) {
            drawComponent = formView.get(property.getToDraw(this)).grid;
        } else
            drawComponent = formView.get(property);
        return drawComponent.getTabContainer();
    }

    public void finalizeAroundInit() {
        super.finalizeAroundInit();
                        
        groups.finalizeChanges();
        treeGroups.finalizeChanges();
        propertyDraws.finalizeChanges();
        fixedFilters.finalizeChanges();
        eventActions.finalizeChanges();
        defaultOrders.finalizeChanges();
        fixedOrders.finalizeChanges();
        
        hintsIncrementTable.finalizeChanges();
        hintsNoUpdate.finalizeChanges();
        
        for(RegularFilterGroupEntity regularFilterGroup : getRegularFilterGroupsIt())
            regularFilterGroup.finalizeAroundInit();
        
        getRichDesign().finalizeAroundInit();
    }

    public static class ComponentSet extends AddSet<ComponentView, ComponentSet> {

        public ComponentSet() {
        }

        public ComponentSet(ComponentView where) {
            super(where);
        }

        public ComponentSet(ComponentView[] wheres) {
            super(wheres);
        }

        protected ComponentSet createThis(ComponentView[] wheres) {
            return new ComponentSet(wheres);
        }

        protected ComponentView[] newArray(int size) {
            return new ComponentView[size];
        }

        protected boolean containsAll(ComponentView who, ComponentView what) {
            return who.isAncestorOf(what);
        }

        public ComponentSet addItem(ComponentView container) {
            return add(new ComponentSet(container));
        }
        
        public ComponentSet addAll(ComponentSet set) {
            return add(set);            
        }
    }
    @IdentityStrongLazy
    public ComponentSet getDrawTabContainers(GroupObjectEntity group) {
        ComponentSet result = new ComponentSet();
        for(PropertyDrawEntity property : getPropertyDrawsIt())
            if(!group.getObjects().disjoint(property.propertyObject.mapping.values().toSet())) { // для свойств "зависящих" от группы
                ComponentView drawContainer = getDrawTabContainer(property, true);
                if(drawContainer==null) // cheat \ оптимизация
                    return null;
                result = result.addItem(drawContainer);

                drawContainer = getDrawTabContainer(property, false);
                if(drawContainer==null) // cheat \ оптимизация
                    return null;
                result = result.addItem(drawContainer);
            }
        ImSet<FilterEntity> fixedFilters = getFixedFilters();
        MSet<GroupObjectEntity> mFixedGroupObjects = SetFact.mSetMax(fixedFilters.size());
        for(FilterEntity filterEntity : fixedFilters) {
            if(!group.getObjects().disjoint(SetFact.fromJavaSet(filterEntity.getObjects()))) { // для фильтров "зависящих" от группы
                GroupObjectEntity drawGroup = filterEntity.getToDraw(this);
                if(!drawGroup.equals(group))
                    mFixedGroupObjects.add(drawGroup); 
            }
        }
        for(GroupObjectEntity fixedGroupObject : mFixedGroupObjects.immutable()) {
            ComponentSet drawContainers = getDrawTabContainers(fixedGroupObject);
            if(drawContainers==null)
                return null;
                
            result = result.addAll(drawContainers);
        }
        return result;
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, DataInputStream inStream) {
        try {
            FormEntity form = new ServerSerializationPool(new ServerContext(BL)).deserializeObject(inStream);
            form.richDesign = NFFact.finalProperty(new ServerSerializationPool(new ServerContext(BL, form)).deserializeObject(inStream));

            return form;
        } catch (IOException e) {
            throw new RuntimeException(ServerResourceBundle.getString("form.entity.error.on.deserialization.form.on.the.server"), e);
        }
    }

    public void setForceViewType(LP property, ClassViewType type) {
        setForceViewType(property.property, type);
    }

    protected void setForceViewType(AbstractNode group, ClassViewType type, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if ((groupObject == null || groupObject.equals(propertyDraw.getToDraw(this))) && group.hasChild(propertyDraw.propertyObject.property)) {
                setForceViewType(propertyDraw, type);
            }
        }
    }

    protected void setForceViewType(AbstractNode group, ClassViewType type) {
        setForceViewType(group, type, null);
    }

    protected void setForceViewType(PropertyDrawEntity propertyDraw, ClassViewType type) {
        propertyDraw.forceViewType = type;
    }

    public List<PropertyDrawEntity> getProperties(AbstractNode group) {
        return getProperties(group, null);
    }

    public List<PropertyDrawEntity> getProperties(AbstractNode group, GroupObjectEntity groupObject) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            if ((groupObject == null || groupObject.equals(property.getToDraw(this))) && group.hasChild(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(Property prop, GroupObjectEntity groupObject) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            if (groupObject.equals(property.getToDraw(this)) && prop.equals(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(Property prop) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            if (prop.equals(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            if (groupObject.equals(property.getToDraw(this))) {
                result.add(property);
            }
        }

        return result;
    }

    public void setReadOnlyIf(CalcPropertyObjectEntity condition) {
        for (PropertyDrawEntity propertyView : getPropertyDrawsIt()) {
            if (propertyView != getPropertyDraw(condition)) {
                setReadOnlyIf(propertyView, condition);
            }
        }
    }

    public void setReadOnlyIf(LP property, CalcPropertyObjectEntity condition) {
        setReadOnlyIf(getPropertyDraw(property.property), condition);
    }

    public void setReadOnlyIf(GroupObjectEntity groupObject, CalcPropertyObjectEntity condition) {
        for (PropertyDrawEntity propertyView : getProperties(groupObject)) {
            if (propertyView != getPropertyDraw(condition)) {
                setReadOnlyIf(propertyView, condition);
            }
        }
    }

    public void setReadOnlyIf(PropertyDrawEntity property, CalcPropertyObjectEntity condition) {
        property.propertyReadOnly = condition;
    }

    public void setEditType(AbstractGroup group, PropertyEditType editType, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity property : getProperties(group, groupObject)) {
            setEditType(property, editType);
        }
    }

    public void setEditType(LP property, PropertyEditType editType) {
        setEditType(property.property, editType);
    }

    public void setEditType(LP property, PropertyEditType editType, GroupObjectEntity groupObject) {
        setEditType(property.property, editType, groupObject);
    }

    public void setEditType(AbstractNode property, PropertyEditType editType) {
        for (PropertyDrawEntity propertyView : getProperties(property)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(Property property, PropertyEditType editType, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity propertyView : getProperties(property, groupObject)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(PropertyEditType editType, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity propertyView : getProperties(groupObject)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(PropertyEditType editType) {
        for (PropertyDrawEntity propertyView : getPropertyDrawsIt()) {
            setEditType(propertyView, editType);
        }
    }

    public void setNFEditType(PropertyEditType editType, Version version) {
        for (PropertyDrawEntity propertyView : getNFPropertyDrawsIt(version)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(ObjectEntity objectEntity, PropertyEditType editType) {
        for (PropertyDrawEntity property : getProperties(objectEntity.groupTo)) {
            setEditType(property, editType);
        }
    }

    public void setEditType(PropertyDrawEntity property, PropertyEditType editType) {
        property.setEditType(editType);
    }

    public void addDefaultOrder(LP lp, boolean ascending, Version version) {
        addDefaultOrder(getPropertyDraw(lp), ascending, version);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.add(property, ascending, version);
    }

    public void addDefaultOrderView(PropertyDrawEntity property, boolean ascending, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addDefaultOrder(property, ascending, version);
    }

    public void setPageSize(int pageSize) {
        for (GroupObjectEntity group : getGroupsIt()) {
            group.pageSize = pageSize;
        }
    }

    public void setNeedVerticalScroll(boolean scroll) {
        for (GroupObjectEntity entity : getGroupsIt()) {
            getRichDesign().get(entity).needVerticalScroll = scroll;
        }
    }

    public Collection<ObjectEntity> getObjects() {
        List<ObjectEntity> objects = new ArrayList<ObjectEntity>();
        for (GroupObjectEntity group : getGroupsIt())
            for (ObjectEntity object : group.getObjects())
                objects.add(object);
        return objects;
    }
    
    public String getTitle(){
        return title!=null ? title : caption;
    }
}