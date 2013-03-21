package platform.server.form.entity;

import org.apache.log4j.Logger;
import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.LongMutable;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.base.serialization.CustomSerializable;
import platform.interop.ClassViewType;
import platform.interop.FormEventType;
import platform.interop.ModalityType;
import platform.interop.PropertyEditType;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.IdentityStrongLazy;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
import platform.server.context.ThreadLocalContext;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ComponentView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;
import platform.server.logics.LogicsModule;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.NewSessionActionProperty;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.property.group.AbstractNode;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static platform.server.logics.ServerResourceBundle.getString;

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

    public HashMap<Object, List<ActionPropertyObjectEntity<?>>> eventActions = new HashMap<Object, List<ActionPropertyObjectEntity<?>>>();

    public List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
    public List<TreeGroupEntity> treeGroups = new ArrayList<TreeGroupEntity>();
    public List<PropertyDrawEntity> propertyDraws = new ArrayList<PropertyDrawEntity>();
    public Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();
    public List<RegularFilterGroupEntity> regularFilterGroups = new ArrayList<RegularFilterGroupEntity>();

    public OrderedMap<PropertyDrawEntity<?>,Boolean> defaultOrders = new OrderedMap<PropertyDrawEntity<?>, Boolean>();
    public OrderedMap<OrderEntity<?>, Boolean> fixedOrders = new OrderedMap<OrderEntity<?>, Boolean>();

    public String title;
    public boolean isPrintForm;
    public ModalityType modalityType = ModalityType.DOCKED;

    public boolean isSynchronizedApply = false;

    @SuppressWarnings("UnusedDeclaration")
    public FormEntity() {
    }

    protected FormEntity(String sID, String caption) {
        this(sID, caption, null);
    }
    
    protected FormEntity(String sID, String caption, String icon) {
        this(sID, caption, icon, false);
    }

    FormEntity(String sID, String caption, String icon, boolean iisPrintForm) {
        this(null, sID, caption, null, icon, iisPrintForm);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption) {
        this(parent, sID, caption, null, null, false);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption, String icon) {
        this(parent, sID, caption, null, icon, false);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption, String title, String icon) {
        this(parent, sID, caption, title, icon, false);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption, boolean iisPrintForm) {
        this(parent, sID, caption, null, null, iisPrintForm);
    }

    public FormEntity(NavigatorElement<T> parent, String sID, String caption,  String icon, boolean iisPrintForm) {
        this(parent, sID, caption, null, icon, iisPrintForm);
    }

    protected FormEntity(NavigatorElement<T> parent, String sID, String caption, String ititle, String icon, boolean iisPrintForm) {
        super(parent, sID, caption, null);
        setImage(icon != null ? icon : "/images/form.png");
        logger.debug("Initializing form " + caption + "...");

        title = ititle;
        isPrintForm = iisPrintForm;

        BaseLogicsModule baseLM = ThreadLocalContext.getBusinessLogics().LM;

        printActionPropertyDraw = addPropertyDraw(baseLM.formPrint);
        editActionPropertyDraw = addPropertyDraw(baseLM.formEdit);
        xlsActionPropertyDraw = addPropertyDraw(baseLM.formXls);
        refreshActionPropertyDraw = addPropertyDraw(baseLM.formRefresh);
        applyActionPropertyDraw = addPropertyDraw(baseLM.formApply);
        cancelActionPropertyDraw = addPropertyDraw(baseLM.formCancel);
        okActionPropertyDraw = addPropertyDraw(baseLM.formOk);
        closeActionPropertyDraw = addPropertyDraw(baseLM.formClose);
        dropActionPropertyDraw = addPropertyDraw(baseLM.formDrop);
    }

    public void addFixedFilter(FilterEntity filter) {
        fixedFilters.add(filter);
    }

    public void addFixedOrder(OrderEntity order, boolean descending) {
        fixedOrders.put(order, descending);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group) {
        regularFilterGroups.add(group);
        if (richDesign != null)
            richDesign.addRegularFilterGroup(group);
    }

    // получает свойства, которые изменяют propChanges и соответственно hint'ить нельзя - временная затычка
    public ImSet<CalcProperty> getChangeModifierProps() {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(PropertyDrawEntity propertyDraw : propertyDraws) {
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

    protected RegularFilterGroupEntity addSingleRegularFilterGroup(FilterEntity ifilter, String iname, KeyStroke ikey) {

        RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
        filterGroup.addFilter(new RegularFilterEntity(genID(), ifilter, iname, ikey));
        addRegularFilterGroup(filterGroup);

        return filterGroup;
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
        for (GroupObjectEntity group : groups) {
            if (group.getID() == id) {
                return group;
            }
        }

        return null;
    }

    public GroupObjectEntity getGroupObject(String sID) {
        for (GroupObjectEntity group : groups) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public TreeGroupEntity getTreeGroup(int id) {
        for (TreeGroupEntity treeGroup : treeGroups) {
            if (treeGroup.getID() == id) {
                return treeGroup;
            }
        }

        return null;
    }

    public ObjectEntity getObject(int id) {
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getObject(String sid) {
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getObject(ValueClass cls) {
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.getObjects()) {
                if (cls.equals(object.baseClass)) {
                    return object;
                }
            }
        }
        return null;
    }

    public List<String> getObjectsNames() {
        List<String> names = new ArrayList<String>();
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.getObjects()) {
                names.add(object.getSID());
            }
        }
        return names;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(String sid) {
        if (sid == null) {
            return null;
        }

        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            if (sid.equals(filterGroup.getSID())) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterEntity getRegularFilter(int id) {
        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            for (RegularFilterEntity filter : filterGroup.filters) {
                if (filter.getID() == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    @IdentityLazy
    public boolean isReadOnly() {
        for (PropertyDrawEntity property : propertyDraws) {
            if (!property.isReadOnly() && !(property.propertyObject.property instanceof NewSessionActionProperty)) {
                return false;
            }
        }

        return true;
    }

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, String caption, Object... groups) {
        return addSingleGroupObject(genID(), (String) null, baseClass, caption, groups);
    }

    protected ObjectEntity addSingleGroupObject(String sID, ValueClass baseClass, String caption, Object... groups) {
        return addSingleGroupObject(genID(), sID, baseClass, caption, groups);
    }

    protected ObjectEntity addSingleGroupObject(int ID, String sID, ValueClass baseClass, String caption, Object... groups) {

        GroupObjectEntity groupObject = new GroupObjectEntity(ID, sID);
        ObjectEntity object = new ObjectEntity(ID, sID, baseClass, caption);
        groupObject.add(object);
        addGroupObject(groupObject);

        addPropertyDraw(groups, false, object);

        return object;
    }

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, Object... groups) {
        return addSingleGroupObject(baseClass, null, groups);
    }

    protected ObjectEntity addSingleGroupObject(int ID, ValueClass baseClass, Object... groups) {
        return addSingleGroupObject(ID, (String) null, baseClass, null, groups);
    }

    protected ObjectEntity addSingleGroupObject(int ID, String sID, ValueClass baseClass, Object... groups) {
        return addSingleGroupObject(ID, sID, baseClass, null, groups);
    }

    public TreeGroupEntity addTreeGroupObject(GroupObjectEntity... tGroups) {
        return addTreeGroupObject((String) null, tGroups);
    }

    public TreeGroupEntity addTreeGroupObject(String sID, GroupObjectEntity... tGroups) {
        TreeGroupEntity treeGroup = new TreeGroupEntity(genID());
        if (sID != null)
            treeGroup.setSID(sID);
        for (GroupObjectEntity group : tGroups) {
            if (!groups.contains(group)) {
                groups.add(group);
            }
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup);

        if (richDesign != null)
            richDesign.addTreeGroup(treeGroup);

        return treeGroup;
    }

    public void addGroupObject(GroupObjectEntity group) {
        // регистрируем ID'шники, чтобы случайно не пересеклись заданные вручную и сгенерированные ID'шники
        idGenerator.idRegister(group.getID());
        for (ObjectEntity obj : group.getObjects()) {
            idGenerator.idRegister(obj.getID());
        }

        for (GroupObjectEntity groupOld : groups) {
            assert group.getID() != groupOld.getID() && !group.getSID().equals(groupOld.getSID());
            for (ObjectEntity obj : group.getObjects()) {
                for (ObjectEntity objOld : groupOld.getObjects()) {
                    assert obj.getID() != objOld.getID() && !obj.getSID().equals(objOld.getSID());
                }
            }
        }
        groups.add(group);

        if (richDesign != null)
            richDesign.addGroupObject(group);
    }

    protected void addPropertyDraw(ObjectEntity object, Object... groups) {
        addPropertyDraw(groups, false, object);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, Object... groups) {
        addPropertyDraw(groups, false, object1, object2);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, Object... groups) {
        addPropertyDraw(groups, false, object1, object2, object3);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, Object... groups) {
        addPropertyDraw(groups, false, object1, object2, object3, object4);
    }

    protected void addPropertyDraw(ObjectEntity object, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2, object3);
    }

    protected void addPropertyDraw(ObjectEntity object1, ObjectEntity object2, ObjectEntity object3, ObjectEntity object4, boolean useObjSubsets, Object... groups) {
        addPropertyDraw(groups, useObjSubsets, object1, object2, object3, object4);
    }

    private void addPropertyDraw(Object[] groups, boolean useObjSubsets, ObjectEntity... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) {
//                continue;
            } else if (group instanceof AbstractNode) {
                boolean upClasses = false;
                if ((i + 1) < groups.length && groups[i + 1] instanceof Boolean) {
                    upClasses = (Boolean) groups[i + 1];
                }
                addPropertyDraw((AbstractNode) group, upClasses, useObjSubsets, objects);
            } else if (group instanceof LP) {
                this.addPropertyDraw((LP) group, objects);
            } else if (group instanceof LP[]) {
                this.addPropertyDraw((LP[]) group, objects);
            }
        }
    }

    public List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, ObjectEntity... objects) {
        return addPropertyDraw(group, upClasses, null, false, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, boolean useObjSubsets, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, useObjSubsets, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, groupObject, false, objects);
    }

    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, ObjectEntity... objects) {
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
        for (PropertyClassImplement implement : group.getProperties(classSubsets, upClasses)) {
            propertyDraws.add(addPropertyDraw(implement.createLP(orderInterfaces.filterOrderIncl(implement.mapping.valuesSet())), groupObject, orderObjects.toArray(new ObjectEntity[orderObjects.size()])));
        }

        return propertyDraws;
    }

    public PropertyDrawEntity addPropertyDraw(LP property, PropertyObjectInterfaceEntity... objects) {
        return addPropertyDraw(property, null, objects);
    }

    public void addPropertyDraw(LP[] properties, ObjectEntity... objects) {
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
            addPropertyDraw(property, objects);
        }
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P, ?> property, GroupObjectEntity groupObject, PropertyObjectInterfaceEntity... objects) {

        return addPropertyDraw(groupObject, property.createObjectEntity(objects));
    }

    public GroupObjectEntity getApplyObject(Collection<ObjectEntity> objects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.getObjects()) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(Property<P> property) {
        return addPropertyDraw(property, MapFact.<P, PropertyObjectInterfaceEntity>EMPTY());
    }

    public <I extends PropertyInterface, P extends Property<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImMap<I, ? extends PropertyObjectInterfaceEntity> mapping) {
        return addPropertyDraw(null, PropertyObjectEntity.create(property, mapping, null, null));
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P, ?> propertyImplement) {
        PropertyDrawEntity<P> newPropertyDraw = new PropertyDrawEntity<P>(genID(), propertyImplement, groupObject);

        propertyImplement.property.proceedDefaultDraw(newPropertyDraw, this);

        if (propertyImplement.property.getSID() != null) {
            String propertySID = BaseUtils.nvl(propertyImplement.property.getName(), propertyImplement.property.getSID());

            setPropertyDrawGeneratedSID(newPropertyDraw, propertySID);
        }

        int ind = propertyDraws.size() - 1;
        if (!newPropertyDraw.shouldBeLast) {
            while (ind >= 0) {
                PropertyDrawEntity property = propertyDraws.get(ind);
                if (!property.shouldBeLast) {
                    break;
                }
                --ind;
            }
        }
        propertyDraws.add(ind + 1, newPropertyDraw);

        return newPropertyDraw;
    }

    public void addPropertyDrawView(PropertyDrawEntity propertyDraw) {
        if (richDesign != null) {
            richDesign.addPropertyDraw(propertyDraw);
        }
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour) {
        propertyDraws.remove(property);
        int neighbourIndex = propertyDraws.indexOf(newNeighbour);
        if (isRightNeighbour) {
            ++neighbourIndex;
        }
        if (newNeighbour.shouldBeLast) {   // поддерживаем shouldBeLast на всякий случай
            property.shouldBeLast = true;
        }
        propertyDraws.add(neighbourIndex, property);

        if (richDesign != null) {
            richDesign.movePropertyDrawTo(property, newNeighbour, isRightNeighbour);
        }
    }

    public void setPropertyDrawGeneratedSID(PropertyDrawEntity property, String baseSID) {
        assert baseSID != null;

        property.setSID(null);

        String sidToSet = baseSID;
        int cnt = 0;

        while (getPropertyDraw(sidToSet) != null) {
            sidToSet = baseSID + (++cnt);
        }

        property.setSID(sidToSet);
    }

    public <P extends PropertyInterface> void removePropertyDraw(PropertyDrawEntity<P> property) {
        propertyDraws.remove(property);
    }

    protected <P extends PropertyInterface> void removePropertyDraw(LP<P, ?> property) {
        removePropertyDraw(property.property);
    }

    protected void removePropertyDraw(AbstractNode group) {
        Iterator<PropertyDrawEntity> it = propertyDraws.iterator();
        while (it.hasNext()) {
            if (group.hasChild(it.next().propertyObject.property)) {
                it.remove();
            }
        }
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
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDraw(String sid) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (sid.equals(propertyDraw.getSID())) {
                return propertyDraw;
            }
        }

        return null;
    }

    public List<PropertyDrawEntity> getPropertyDrawList(LP...properties) {
        List<PropertyDrawEntity> list = new ArrayList<PropertyDrawEntity>();
        for (LP property : properties) {
            list.add(getPropertyDraw(property));
        }
        return list;
    }


    protected List<PropertyDrawEntity> getPropertyDrawList(ObjectEntity object, LP... properties) {
        return getPropertyDrawList(object.groupTo, properties);
    }

    public List<PropertyDrawEntity> getPropertyDrawList(GroupObjectEntity groupObject, LP... properties) {
        List<PropertyDrawEntity> list = new ArrayList<PropertyDrawEntity>();
        for (LP property : properties) {
            for (PropertyDrawEntity<?> propertyDraw : propertyDraws) {
                if ((propertyDraw.propertyObject.property.equals(property.property))&&(groupObject.equals(propertyDraw.getToDraw(this)))) {
                    list.add(propertyDraw);
                    break;
                }
            }
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

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?, ?> lp, ObjectEntity object) {
        return getPropertyDraw(lp.property, object.groupTo);
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?, ?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp.property, groupObject);
    }

    protected PropertyDrawEntity getPropertyDraw(PropertyObjectEntity property) {

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
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
        for (PropertyDrawEntity<?> propertyDraw : propertyDraws) {
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

    /**
     * ищет PropertyDrawEntity, который мэпит на входы LP переданные objects
     */
    public PropertyDrawEntity getPropertyDraw(LP lp, PropertyObjectInterfaceEntity... objects) {
        if (lp.listInterfaces.size() != objects.length) {
            return null;
        }

        for (PropertyDrawEntity propertyDraw : propertyDraws) {
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
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (group.hasChild(propertyDraw.propertyObject.property) && groupObject.equals(propertyDraw.getToDraw(this))) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public void finalizeHints() {
        finalizedHints = true;
        hintsIncrementTable = ((MExclSet<CalcProperty>)hintsIncrementTable).immutable();
        hintsNoUpdate = ((MSet<CalcProperty>)hintsNoUpdate).immutable();
    }
    
    private boolean finalizedHints;

    private Object hintsIncrementTable = SetFact.mExclSet();
    @LongMutable
    public ImSet<CalcProperty> getHintsIncrementTable() {
        if(!finalizedHints)
            finalizeHints();

        return (ImSet<CalcProperty>) hintsIncrementTable;
    }

    public void addHintsIncrementTable(LCP... props) {
        assert !finalizedHints;
        for (LP prop : props) {
            ((MExclSet<CalcProperty>)hintsIncrementTable).exclAdd((CalcProperty) prop.property);
        }
    }

    public void addHintsIncrementTable(CalcProperty... props) {
        assert !finalizedHints;
        for (CalcProperty prop : props) {
            ((MExclSet<CalcProperty>)hintsIncrementTable).exclAdd(prop);
        }
    }

    private Object hintsNoUpdate = SetFact.mSet();
    @LongMutable
    public ImSet<CalcProperty> getHintsNoUpdate() {
        if(!finalizedHints)
            finalizeHints();
        
        return (ImSet<CalcProperty>) hintsNoUpdate;
    }

    @IdentityLazy
    public FunctionSet<CalcProperty> getNoHints() {
        if (Settings.get().isDisableChangeModifierAllHints())
            return BaseUtils.universal(getChangeModifierProps().isEmpty());
        else
            return CalcProperty.getDependsOnSet(getChangeModifierProps()); // тут какая то проблема есть
    }

    public void addHintsNoUpdate(GroupObjectEntity groupObject) {
        assert !finalizedHints;
        for (PropertyDrawEntity property : getProperties(groupObject)) {
            if (property.propertyObject.property instanceof CalcProperty) {
                addHintsNoUpdate((CalcProperty) property.propertyObject.property);
            }
        }
    }

    public void addHintsNoUpdate(AbstractGroup group) {
        assert !finalizedHints;
        for (Property property : group.getProperties()) {
            if (property instanceof CalcProperty) {
                addHintsNoUpdate((CalcProperty) property);
            }
        }
    }

    public void addHintsNoUpdate(LCP... props) {
        for (LCP prop : props) {
            addHintsNoUpdate(prop);
        }
    }

    protected void addHintsNoUpdate(LCP prop) {
        addHintsNoUpdate((CalcProperty) prop.property);
    }

    public void addHintsNoUpdate(CalcProperty prop) {
        assert !finalizedHints;
        ((MSet<CalcProperty>)hintsNoUpdate).add(prop);
    }

    public FormView createDefaultRichDesign() {
        return new DefaultFormView(this);
    }

    private FormView richDesign;

    public FormView getRichDesign() {
        if (richDesign == null) {
            richDesign = createDefaultRichDesign();
        }
        return richDesign;
    }

    public void setRichDesign(FormView view) {
        richDesign = view;
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

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();

    public byte getTypeID() {
        return 0;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        pool.writeString(outStream, title);
        pool.writeString(outStream, sID);
        outStream.writeBoolean(isPrintForm);
        outStream.writeUTF(modalityType.name());

        pool.serializeCollection(outStream, groups);
        pool.serializeCollection(outStream, treeGroups);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);

        pool.serializeObject(outStream, printActionPropertyDraw);
        pool.serializeObject(outStream, editActionPropertyDraw);
        pool.serializeObject(outStream, xlsActionPropertyDraw);
        pool.serializeObject(outStream, dropActionPropertyDraw);
        pool.serializeObject(outStream, refreshActionPropertyDraw);
        pool.serializeObject(outStream, applyActionPropertyDraw);
        pool.serializeObject(outStream, cancelActionPropertyDraw);
        pool.serializeObject(outStream, okActionPropertyDraw);
        pool.serializeObject(outStream, closeActionPropertyDraw);

        outStream.writeInt(defaultOrders.size());
        for (Map.Entry<PropertyDrawEntity<?>, Boolean> entry : defaultOrders.entrySet()) {
            pool.serializeObject(outStream, entry.getKey(), serializationType);
            outStream.writeBoolean(entry.getValue());
        }

        outStream.writeInt(eventActions.size());
        for (Map.Entry<Object, List<ActionPropertyObjectEntity<?>>> entry : eventActions.entrySet()) {
            Object event = entry.getKey();

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

            pool.serializeCollection(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
        title = pool.readString(inStream);
        sID = pool.readString(inStream);
        isPrintForm = inStream.readBoolean();
        modalityType = ModalityType.valueOf(inStream.readUTF());

        groups = pool.deserializeList(inStream);
        treeGroups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        regularFilterGroups = pool.deserializeList(inStream);

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
        for (int i = 0; i < orderCount; i++) {
            PropertyDrawEntity order = pool.deserializeObject(inStream);
            defaultOrders.put(order, inStream.readBoolean());
        }

        eventActions = new HashMap<Object, List<ActionPropertyObjectEntity<?>>>();
        int length = inStream.readInt();
        for (int i = 0; i < length; ++i) {
            Object event;
            switch (inStream.readByte()) {
                case 0 : event = pool.readString(inStream); break;
                case 1 : event = pool.deserializeObject(inStream); break;
                default : event = pool.readObject(inStream); break;
            }

            List<ActionPropertyObjectEntity<?>> actions = pool.deserializeList(inStream);
            eventActions.put(event, actions);
        }
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(isPrintForm);
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

        addActionsOnEvent(addPropertyObject(lm.getFormAddObjectAction(entity)), events);
    }

    public void addActionsOnObjectChange(ObjectEntity object, ActionPropertyObjectEntity... actions) {
        addActionsOnObjectChange(object, false, actions);
    }

    public void addActionsOnObjectChange(ObjectEntity object, boolean drop, ActionPropertyObjectEntity... actions) {
        addActionsOnEvent(object, drop, actions);
    }

    public void addActionsOnEvent(ActionPropertyObjectEntity action, Object... events) {
        for(Object event : events)
            addActionsOnEvent(event, action);
    }

    public void addActionsOnEvent(Object eventObject, ActionPropertyObjectEntity<?>... actions) {
        addActionsOnEvent(eventObject, false, actions);
    }

    public void addActionsOnEvent(Object eventObject, boolean drop, ActionPropertyObjectEntity<?>... actions) {
        List<ActionPropertyObjectEntity<?>> thisEventActions = eventActions.get(eventObject);
        if (thisEventActions == null || drop) {
            thisEventActions = new ArrayList<ActionPropertyObjectEntity<?>>();
            eventActions.put(eventObject, thisEventActions);
        }

        thisEventActions.addAll(Arrays.asList(actions));
    }

    public List<ActionPropertyObjectEntity<?>> getActionsOnEvent(Object eventObject) {
        return eventActions.get(eventObject);
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
    }
    @IdentityStrongLazy
    public ComponentSet getDrawTabContainers(GroupObjectEntity group) {
        ComponentSet result = new ComponentSet();
        for(PropertyDrawEntity property : propertyDraws)
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
        return result;
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, DataInputStream inStream) {
        try {
            FormEntity form = new ServerSerializationPool(new ServerContext(BL)).deserializeObject(inStream);
            form.richDesign = new ServerSerializationPool(new ServerContext(BL, form)).deserializeObject(inStream);

            return form;
        } catch (IOException e) {
            throw new RuntimeException(ServerResourceBundle.getString("form.entity.error.on.deserialization.form.on.the.server"), e);
        }
    }

    public void setForceViewType(LP property, ClassViewType type) {
        setForceViewType(property.property, type);
    }

    protected void setForceViewType(AbstractNode group, ClassViewType type, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
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

        for (PropertyDrawEntity property : propertyDraws) {
            if ((groupObject == null || groupObject.equals(property.getToDraw(this))) && group.hasChild(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(Property prop, GroupObjectEntity groupObject) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : propertyDraws) {
            if (groupObject.equals(property.getToDraw(this)) && prop.equals(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(Property prop) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : propertyDraws) {
            if (prop.equals(property.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawEntity> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawEntity> result = new ArrayList<PropertyDrawEntity>();

        for (PropertyDrawEntity property : propertyDraws) {
            if (groupObject.equals(property.getToDraw(this))) {
                result.add(property);
            }
        }

        return result;
    }

    public void setReadOnlyIf(CalcPropertyObjectEntity condition) {
        for (PropertyDrawEntity propertyView : propertyDraws) {
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
        for (PropertyDrawEntity propertyView : propertyDraws) {
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

    public void addDefaultOrder(LP lp, boolean ascending) {
        addDefaultOrder(getPropertyDraw(lp), ascending);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending) {
        defaultOrders.put(property, ascending);
    }

    public void addDefaultOrderView(PropertyDrawEntity property, boolean ascending) {

        if(richDesign!=null)
            richDesign.addDefaultOrder(property, ascending);
    }

    public void setPageSize(int pageSize) {
        for (GroupObjectEntity group : groups) {
            group.pageSize = pageSize;
        }
    }

    public void setNeedVerticalScroll(boolean scroll) {
        for (GroupObjectEntity entity : groups) {
            getRichDesign().get(entity).needVerticalScroll = scroll;
        }
    }

    public Collection<ObjectEntity> getObjects() {
        List<ObjectEntity> objects = new ArrayList<ObjectEntity>();
        for (GroupObjectEntity group : groups)
            for (ObjectEntity object : group.getObjects())
                objects.add(object);
        return objects;
    }
    
    public String getTitle(){
        return title!=null ? title : caption;
    }
}