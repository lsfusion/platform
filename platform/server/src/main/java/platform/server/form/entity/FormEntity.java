package platform.server.form.entity;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Subsets;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.base.serialization.CustomSerializable;
import platform.interop.ClassViewType;
import platform.interop.action.ClientResultAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.RegularFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyClassImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.ValueClassWrapper;
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

public class FormEntity<T extends BusinessLogics<T>> extends NavigatorElement<T> implements ServerIdentitySerializable {
    private final static Logger logger = Logger.getLogger(FormEntity.class);
    private static ImageIcon image = new ImageIcon(NavigatorElement.class.getResource("/images/form.gif"));

    public static final String ON_APPLY_EVENT = "onApplyEvent";
    public static final String ON_OK_EVENT = "onOkEvent";

    public Map<Object, List<PropertyObjectEntity>> eventActions = new HashMap<Object, List<PropertyObjectEntity>>();

    public List<GroupObjectEntity> groups = new ArrayList<GroupObjectEntity>();
    public List<TreeGroupEntity> treeGroups = new ArrayList<TreeGroupEntity>();
    public List<PropertyDrawEntity> propertyDraws = new ArrayList<PropertyDrawEntity>();
    public Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();
    public List<RegularFilterGroupEntity> regularFilterGroups = new ArrayList<RegularFilterGroupEntity>();

    public OrderedMap<OrderEntity<?>, Boolean> fixedOrders = new OrderedMap<OrderEntity<?>, Boolean>();

    public boolean isPrintForm;

    public FormEntity() {
    }

    protected FormEntity(String sID, String caption) {
        this(sID, caption, false);
    }

    FormEntity(String sID, String caption, boolean iisPrintForm) {
        this(null, sID, caption, iisPrintForm);
    }

    protected FormEntity(NavigatorElement<T> parent, String sID, String caption) {
        this(parent, sID, caption, false);
    }

    protected FormEntity(NavigatorElement<T> parent, String sID, String caption, boolean iisPrintForm) {
        super(parent, sID, caption);
        logger.info("Initializing form " + caption + "...");

        isPrintForm = iisPrintForm;
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean shouldProceedDefaultDraw() {
        return true;
    }

    public void addFixedFilter(FilterEntity filter) {
        fixedFilters.add(filter);
    }

    public void addFixedOrder(OrderEntity order, boolean descending) {
        fixedOrders.put(order, descending);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group) {
        regularFilterGroups.add(group);
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
            for (ObjectEntity object : group.objects) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : regularFilterGroups) {
            if (filterGroup.getID() == id) {
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

    protected ObjectEntity addSingleGroupObject(ValueClass baseClass, String caption, Object... groups) {
        return addSingleGroupObject(genID(), (String) null, baseClass, caption, groups);
    }

    protected ObjectEntity addSingleGroupObject(int ID, String sID, ValueClass baseClass, String caption, Object... groups) {

        GroupObjectEntity groupObject = new GroupObjectEntity(ID, sID);
        ObjectEntity object = new ObjectEntity(ID, sID, baseClass, caption);
        groupObject.add(object);
        addGroup(groupObject);

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

    protected TreeGroupEntity addTreeGroupObject(GroupObjectEntity... tGroups) {
        TreeGroupEntity treeGroup = new TreeGroupEntity(genID());
        for (GroupObjectEntity group : tGroups) {
            if (!groups.contains(group)) {
                groups.add(group);
            }
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup);
        return treeGroup;
    }

    protected void addGroup(GroupObjectEntity group) {
        groups.add(group);
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
            } else if (group instanceof LP[])
                this.addPropertyDraw((LP[])group, objects);
        }
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, false, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, boolean useObjSubsets, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, null, useObjSubsets, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, ObjectEntity... objects) {
        addPropertyDraw(group, upClasses, groupObject, false, objects);
    }

    protected void addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, ObjectEntity... objects) {
        List<ValueClassWrapper> valueClasses = new ArrayList<ValueClassWrapper>();
        Map<ObjectEntity, ValueClassWrapper> objectToClass = new HashMap<ObjectEntity, ValueClassWrapper>();
        for (ObjectEntity object : objects) {
            ValueClassWrapper wrapper = new ValueClassWrapper(object.baseClass);
            valueClasses.add(wrapper);
            objectToClass.put(object, wrapper);
        }

        List<List<ValueClassWrapper>> classSubsets;
        if (useObjSubsets) {
            classSubsets = new ArrayList<List<ValueClassWrapper>>();
            for (Set<ValueClassWrapper> set : new Subsets<ValueClassWrapper>(valueClasses)) {
                List<ValueClassWrapper> objectList = new ArrayList<ValueClassWrapper>(set);
                if (!objectList.isEmpty()) {
                    classSubsets.add(objectList);
                }
            }
        } else {
            classSubsets = Collections.singletonList(valueClasses);
        }

        for (PropertyClassImplement implement : group.getProperties(classSubsets, upClasses)) {
            List<PropertyInterface> interfaces = new ArrayList<PropertyInterface>();
            Map<ObjectEntity, PropertyInterface> objectToInterface =
                    BaseUtils.<ObjectEntity, ValueClassWrapper, PropertyInterface>join(objectToClass, BaseUtils.reverse(implement.mapping));
            for (ObjectEntity object : objects) {
                interfaces.add(objectToInterface.get(object));
            }
            addPropertyDraw(new LP(implement.property, interfaces), groupObject, objects);
        }
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

    protected <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P> property, GroupObjectEntity groupObject, PropertyObjectInterfaceEntity... objects) {

        return addPropertyDraw(groupObject, new PropertyObjectEntity<P>(property, objects));
    }

    public GroupObjectEntity getApplyObject(Collection<ObjectEntity> objects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : groups) {
            for (ObjectEntity object : group.objects) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(Property<P> property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        return addPropertyDraw(null, new PropertyObjectEntity<P>(property, mapping));
    }

    private Map<String, Integer> propertySIDCount = new HashMap<String, Integer>();

    private <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P> propertyImplement) {
        PropertyDrawEntity<P> propertyDraw = new PropertyDrawEntity<P>(genID(), propertyImplement, groupObject);
        if (shouldProceedDefaultDraw()) {
            propertyImplement.property.proceedDefaultDraw(propertyDraw, this);
        }

        if (propertyImplement.property.getSID() != null) {
            String propertyDrawSID = propertyImplement.property.getSID();
            Integer cnt = propertySIDCount.get(propertyDrawSID);
            if (cnt == null) {
                propertySIDCount.put(propertyDrawSID, 1);
            } else {
                ++cnt;
                propertySIDCount.put(propertyDrawSID, cnt);
                propertyDrawSID = propertyDrawSID + cnt;
            }
            propertyDraw.setSID(propertyDrawSID);
        }

        if (propertyDraw.shouldBeLast) {
            propertyDraws.add(propertyDraw);
        } else {
            int count = 0;
            for (PropertyDrawEntity property : propertyDraws) {
                if (property.shouldBeLast) {
                    propertyDraws.add(count, propertyDraw);
                    count = -1;
                    break;
                }
                count++;
            }

            if (count >= 0) {
                propertyDraws.add(propertyDraw);
            }
        }


        assert richDesign == null;

        return propertyDraw;
    }

    protected <P extends PropertyInterface> void removePropertyDraw(LP<P> property) {
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

        return new PropertyObjectEntity(property, objects);
    }

    public PropertyDrawEntity<?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : propertyDraws) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp) {
        return getPropertyDraw(lp).propertyObject;
    }

    public PropertyDrawEntity<?> getPropertyDraw(LP<?> lp) {
        return getPropertyDraw(lp.property);
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp, object).propertyObject;
    }

    protected PropertyObjectEntity getPropertyObject(LP<?> lp, GroupObjectEntity groupObject) {
        return getPropertyDraw(lp, groupObject).propertyObject;
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, ObjectEntity object) {
        return getPropertyDraw(lp.property, object.groupTo);
    }

    protected PropertyDrawEntity<?> getPropertyDraw(LP<?> lp, GroupObjectEntity groupObject) {
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

        PropertyDrawEntity resultPropertyDraw = null;
        for (PropertyDrawEntity<?> propertyDraw : propertyDraws) {
            if (propertyDraw.propertyObject.property == property) {
                resultPropertyDraw = propertyDraw;
            }
        }

        return resultPropertyDraw;
    }

    public PropertyDrawEntity getPropertyDraw(AbstractNode group, ObjectEntity object) {
        return getPropertyDraw(group, object.groupTo);
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

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();

    public void addHintsNoUpdate(GroupObjectEntity groupObject) {
        for (PropertyDrawEntity property : getProperties(groupObject)) {
            addHintsNoUpdate(property.propertyObject.property);
        }
    }

    public void addHintsNoUpdate(AbstractGroup group) {
        for (Property property : group.getProperties()) {
            addHintsNoUpdate(property);
        }
    }

    protected void addHintsNoUpdate(LP<?> prop) {
        addHintsNoUpdate(prop.property);
    }

    protected void addHintsNoUpdate(Property prop) {
        if (!hintsNoUpdate.contains(prop))
            hintsNoUpdate.add(prop);
    }

    public Map<PropertyDrawEntity, GroupObjectEntity> forceDefaultDraw = new HashMap<PropertyDrawEntity, GroupObjectEntity>();

    public FormView createDefaultRichDesign() {
        return new DefaultFormView(this);
    }

    public FormView richDesign;

    public FormView getRichDesign() {
        if (richDesign == null) {
            return createDefaultRichDesign();
        } else {
            return richDesign;
        }
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
        pool.writeString(outStream, sID);
        outStream.writeBoolean(isPrintForm);

        pool.serializeCollection(outStream, groups);
        pool.serializeCollection(outStream, treeGroups);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
        pool.serializeCollection(outStream, regularFilterGroups);
        pool.serializeMap(outStream, forceDefaultDraw);

        outStream.writeInt(eventActions.size());
        for (Map.Entry<Object, List<PropertyObjectEntity>> entry : eventActions.entrySet()) {
            Object event = entry.getKey();

            //пока предполагаем, что евент либо String, либо CustomSerializable!
            boolean isStringEvent = event instanceof String;
            outStream.writeBoolean(isStringEvent);
            if (isStringEvent) {
                pool.writeString(outStream, (String) event);
            } else {
                pool.serializeObject(outStream, (CustomSerializable) event);
            }

            pool.serializeCollection(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);
        sID = pool.readString(inStream);
        isPrintForm = inStream.readBoolean();

        groups = pool.deserializeList(inStream);
        treeGroups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);
        regularFilterGroups = pool.deserializeList(inStream);
        forceDefaultDraw = pool.deserializeMap(inStream);

        eventActions = new HashMap<Object, List<PropertyObjectEntity>>();
        int length = inStream.readInt();
        for (int i = 0; i < length; ++i) {
            Object event = inStream.readBoolean()
                    ? pool.readString(inStream)
                    : pool.deserializeObject(inStream);

            List<PropertyObjectEntity> actions = pool.deserializeList(inStream);
            eventActions.put(event, actions);
        }
    }

    @Override
    public void serialize(DataOutputStream outStream, Collection<NavigatorElement> elements) throws IOException {
        super.serialize(outStream, elements);
        outStream.writeBoolean(isPrintForm);
    }

    public void addActionsOnObjectChange(ObjectEntity object, PropertyObjectEntity... actions) {
        addActionsOnObjectChange(object, false, actions);
    }

    public void addActionsOnObjectChange(ObjectEntity object, boolean drop, PropertyObjectEntity... actions) {
        addActionsOnEvent(object, drop, actions);
    }

    public void addActionsOnApply(PropertyObjectEntity... actions) {
        addActionsOnEvent(ON_APPLY_EVENT, false, actions);
    }

    public void addActionsOnOk(PropertyObjectEntity... actions) {
        addActionsOnEvent(ON_OK_EVENT, false, actions);
    }

    public void addActionsOnEvent(Object eventObject, boolean drop, PropertyObjectEntity... actions) {
        List<PropertyObjectEntity> thisEventActions = eventActions.get(eventObject);
        if (thisEventActions == null || drop) {
            thisEventActions = new ArrayList<PropertyObjectEntity>();
            eventActions.put(eventObject, thisEventActions);
        }

        thisEventActions.addAll(Arrays.asList(actions));
    }

    public List<PropertyObjectEntity> getActionsOnEvent(Object eventObject) {
        return eventActions.get(eventObject);
    }

    public boolean hasClientApply() {
        return false;
    }

    public ClientResultAction getClientApply(FormInstance<T> form) {
        return null; // будем возвращать именно null, чтобы меньше данных передавалось        
    }

    public String checkClientApply(Object result) {
        return null;
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, byte[] formState) {
        return deserialize(BL, new DataInputStream(new ByteArrayInputStream(formState)));
    }

    public static FormEntity<?> deserialize(BusinessLogics BL, DataInputStream inStream) {
        try {
            FormEntity form = new ServerSerializationPool(new ServerContext(BL)).deserializeObject(inStream);
            form.richDesign = new ServerSerializationPool(new ServerContext(BL, form)).deserializeObject(inStream);

            return form;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при десериализации формы на сервере", e);
        }
    }

    protected void setForceViewType(LP property, ClassViewType type) {
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

    public void setReadOnly(AbstractGroup group, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawEntity property : getProperties(group, groupObject)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(LP property, boolean readOnly) {
        setReadOnly(property.property, readOnly);
    }

    public void setReadOnly(LP property, boolean readOnly, GroupObjectEntity groupObject) {
        setReadOnly(property.property, readOnly, groupObject);
    }

    public void setReadOnly(AbstractNode property, boolean readOnly) {

        for (PropertyDrawEntity propertyView : getProperties(property)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(Property property, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawEntity propertyView : getProperties(property, groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(boolean readOnly, GroupObjectEntity groupObject) {
        for (PropertyDrawEntity propertyView : getProperties(groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(boolean readOnly) {
        for (PropertyDrawEntity propertyView : propertyDraws) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(ObjectEntity objectEntity, boolean readOnly) {
        for (PropertyDrawEntity property : getProperties(objectEntity.groupTo)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(PropertyDrawEntity property, boolean readOnly) {
        property.readOnly = readOnly;
    }

    public void setPageSize(int pageSize) {
        for (GroupObjectEntity group : groups)
            group.pageSize = pageSize;
    }

    public void setNeedVerticalScroll(boolean scroll) {
        DefaultFormView view = null;
        if (richDesign instanceof DefaultFormView) {
            view = (DefaultFormView) richDesign;
        }
        for (GroupObjectEntity entity : groups) {
            view.get(entity).needVerticalScroll = scroll;
        }
    }

    @Override
    public ImageIcon getImage() {
        return image;
    }
}
