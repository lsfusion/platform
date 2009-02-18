/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

// навигатор работает с абстрактной BL

import net.sf.jasperreports.engine.design.JasperDesign;

import java.sql.SQLException;
import java.util.*;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> {

    DataAdapter Adapter;
    T BL;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(DataAdapter iAdapter,T iBL) {
        this(iAdapter, iBL, new ClassCache());
    }

    public RemoteNavigator(DataAdapter iAdapter, T iBL, ClassCache iclassCache) {
        Adapter = iAdapter;
        BL = iBL;
        classCache = iclassCache;
    }

    User currentUser;
    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    public boolean changeCurrentUser(String login, String password) {

        User user = BL.authPolicy.getUser(login, password);
        if (user == null) return false;

        currentUser = user;
        securityPolicy = BL.authPolicy.getSecurityPolicy(currentUser);

        return true;
    }

    public byte[] getCurrentUserInfoByteArray() {

        if (currentUser == null) return new byte[] {};

        return ByteArraySerializer.serializeUserInfo(currentUser.userInfo);
    }

    public final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    public final static int NAVIGATORGROUP_RELEVANTCLASS = -3;

    List<NavigatorElement> getElements(int elementID) {

        List<NavigatorElement> navigatorElements;
        switch (elementID) {
            case (NAVIGATORGROUP_RELEVANTFORM) :
                if (currentForm == null)
                    navigatorElements = new ArrayList();
                else
                    navigatorElements = new ArrayList(((NavigatorForm)BL.baseElement.getNavigatorElement(currentForm.getID())).relevantElements);
                break;
            case (NAVIGATORGROUP_RELEVANTCLASS) :
                if (currentClass == null)
                    navigatorElements = new ArrayList();
                else
                    navigatorElements = new ArrayList(currentClass.relevantElements);
                break;
            default :
                navigatorElements = getElements(BL.baseElement.getNavigatorElement(elementID));
        }

        List<NavigatorElement> resultElements = new ArrayList();

        for (NavigatorElement element : navigatorElements)
            if (securityPolicy.navigator.checkPermission(element))
                resultElements.add(element);

        return resultElements;
    }

    List<NavigatorElement> getElements(NavigatorElement element) {

        if (element == null) element = BL.baseElement;
        return new ArrayList(element.getChildren());
    }

    public byte[] getElementsByteArray(int groupID) {
        return ByteArraySerializer.serializeListNavigatorElement(getElements(groupID));
    }

    //используется для RelevantFormNavigator
    RemoteForm<T> currentForm;

    //используется для RelevantClassNavigator
    Class currentClass;
    boolean changeCurrentClass(int classID) {

        if (currentClass != null && currentClass.ID == classID) return false;

        currentClass = BL.objectClass.findClassID(classID);
        return true;
    }

//    RemoteForm<T> lastOpenedForm;
    RemoteForm<T> createForm(int formID, boolean currentSession) throws SQLException {

        NavigatorForm navigatorForm = (NavigatorForm)BL.baseElement.getNavigatorElement(formID);

        if (!securityPolicy.navigator.checkPermission(navigatorForm)) return null;

        DataSession session;
        if (currentSession && currentForm != null)
            session = currentForm.session;
        else
            session = BL.createSession(Adapter);

        RemoteForm remoteForm = new RemoteForm(formID, BL, session, securityPolicy) {

            protected void objectChanged(Class cls, Integer objectID) {
                super.objectChanged(cls, objectID);
                addCacheObject(cls, objectID);
            }

            public void gainedFocus() {
                super.gainedFocus();
                currentForm = this;
            }
        };

        ObjectImplementMapper objectMapper = new ObjectImplementMapper();
        GroupObjectImplementMapper groupObjectMapper = new GroupObjectImplementMapper(objectMapper);
        PropertyObjectImplementMapper propertyMapper = new PropertyObjectImplementMapper(objectMapper);
        PropertyViewMapper propertyViewMapper = new PropertyViewMapper(propertyMapper, groupObjectMapper);
        FilterMapper filterMapper = new FilterMapper(objectMapper, propertyMapper);

        remoteForm.groups = new ArrayList();
        for (GroupObjectImplement navigatorGroupObject : (List<GroupObjectImplement>)navigatorForm.Groups) {
            remoteForm.groups.add(groupObjectMapper.doMapping(navigatorGroupObject));
        }

        remoteForm.properties = new ArrayList();
        for (PropertyView navigatorProperty : (List<PropertyView>)navigatorForm.propertyViews) {
            if (securityPolicy.property.view.checkPermission(navigatorProperty.view.property))
                remoteForm.properties.add(propertyViewMapper.doMapping(navigatorProperty));
        }

        remoteForm.fixedFilters = new HashSet();

        for (Filter navigatorFilter : (Set<Filter>)navigatorForm.fixedFilters) {
            remoteForm.fixedFilters.add(filterMapper.doMapping(navigatorFilter));
        }

        remoteForm.regularFilterGroups = new ArrayList();
        for (RegularFilterGroup navigatorGroup : (List<RegularFilterGroup>)navigatorForm.regularFilterGroups) {

            RegularFilterGroup group = new RegularFilterGroup(navigatorGroup.ID);
            for (RegularFilter filter : navigatorGroup.filters)
                group.addFilter(new RegularFilter(filter.ID, filterMapper.doMapping(filter.filter), filter.name, filter.key));

            remoteForm.regularFilterGroups.add(group);
        }

        remoteForm.hintsNoUpdate = navigatorForm.hintsNoUpdate;
        remoteForm.hintsSave = navigatorForm.hintsSave;

        remoteForm.richDesign = navigatorForm.getRichDesign();

        remoteForm.reportDesign = navigatorForm.getReportDesign();

        for (GroupObjectImplement groupObject : (List<GroupObjectImplement>)remoteForm.groups)
            for (ObjectImplement object : groupObject) {
                int objectID = classCache.getObject(object.baseClass);
                if (objectID != -1)
                    remoteForm.userObjectSeeks.put(object, objectID);
            }

        return remoteForm;
    }

    private class ObjectImplementMapper {

        private Map<ObjectImplement, ObjectImplement> mapper = new HashMap();

        ObjectImplement doMapping(ObjectImplement objKey) {

            if (mapper.containsKey(objKey)) return mapper.get(objKey);

            ObjectImplement objValue = new ObjectImplement(objKey.ID, objKey.baseClass);
            objValue.sID = objKey.sID;
            objValue.caption = objKey.caption;

            mapper.put(objKey, objValue);
            return objValue;
        }
    }

    private class GroupObjectImplementMapper {

        private Map<GroupObjectImplement, GroupObjectImplement> mapper = new HashMap();
        ObjectImplementMapper objectMapper;

        public GroupObjectImplementMapper(ObjectImplementMapper iobjectMapper) {
            objectMapper = iobjectMapper;
        }

        GroupObjectImplement doMapping(GroupObjectImplement groupKey) {

            if (mapper.containsKey(groupKey)) return mapper.get(groupKey);

            GroupObjectImplement groupValue = new GroupObjectImplement(groupKey.ID);

            groupValue.Order = groupKey.Order;
            groupValue.pageSize = groupKey.pageSize;
            groupValue.gridClassView = groupKey.gridClassView;
            groupValue.singleViewType = groupKey.singleViewType;
            for (ObjectImplement object : groupKey) {
                groupValue.addObject(objectMapper.doMapping(object));
            }

            mapper.put(groupKey, groupValue);
            return groupValue;
        }
    }

    private class PropertyObjectImplementMapper {

        private Map<PropertyObjectImplement, PropertyObjectImplement> mapper = new HashMap();
        ObjectImplementMapper objectMapper;

        PropertyObjectImplementMapper(ObjectImplementMapper iobjectMapper) {
            objectMapper = iobjectMapper;
        }

        PropertyObjectImplement doMapping(PropertyObjectImplement<?> propKey) {

            if (mapper.containsKey(propKey)) return mapper.get(propKey);

            PropertyObjectImplement propValue = new PropertyObjectImplement(propKey.property);
            for (Map.Entry<? extends PropertyInterface,ObjectImplement> entry : propKey.mapping.entrySet()) {
                propValue.mapping.put(entry.getKey(), objectMapper.doMapping(entry.getValue()));
            }

            mapper.put(propKey, propValue);
            return propValue;
        }
    }

    private class PropertyViewMapper {

        private Map<PropertyView, PropertyView> mapper = new HashMap();
        PropertyObjectImplementMapper propertyMapper;
        GroupObjectImplementMapper groupMapper;

        PropertyViewMapper(PropertyObjectImplementMapper ipropertyMapper, GroupObjectImplementMapper igroupMapper) {
            propertyMapper = ipropertyMapper;
            groupMapper = igroupMapper;
        }

        PropertyView doMapping(PropertyView<?> propKey) {

            if (mapper.containsKey(propKey)) return mapper.get(propKey);

            PropertyView propValue = new PropertyView(propKey.ID, propertyMapper.doMapping(propKey.view), groupMapper.doMapping(propKey.toDraw));
            propValue.sID = propKey.sID;

            mapper.put(propKey, propValue);
            return propValue;
        }
    }

    private class FilterMapper {

        private ObjectImplementMapper objectMapper;
        private PropertyObjectImplementMapper propertyMapper;

        FilterMapper(ObjectImplementMapper iobjectMapper, PropertyObjectImplementMapper ipropertyMapper) {
            objectMapper = iobjectMapper;
            propertyMapper = ipropertyMapper;
        }

        Filter doMapping(Filter filterKey) {

            if (filterKey == null) return null;

            ValueLink value = null;

            ValueLink navigatorValue = filterKey.Value;

            if (navigatorValue instanceof UserValueLink) {
                value = new UserValueLink(((UserValueLink)navigatorValue).Value);
            }

            if (navigatorValue instanceof ObjectValueLink) {
                value = new ObjectValueLink(objectMapper.doMapping(((ObjectValueLink)navigatorValue).Object));
            }

            if (navigatorValue instanceof PropertyValueLink) {
                value = new PropertyValueLink(propertyMapper.doMapping(((PropertyValueLink)navigatorValue).Property));
            }

            return new Filter(propertyMapper.doMapping(filterKey.Property), filterKey.Compare, value);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(int classID, int value) {
        addCacheObject(BL.objectClass.findClassID(classID), value);
    }

    public void addCacheObject(Class cls, Integer value) {
        classCache.put(cls, value);
    }

    public int getCacheObject(int classID) {
        return getCacheObject(BL.objectClass.findClassID(classID));
    }

    public int getCacheObject(Class cls) {
        return classCache.getObject(cls);
    }

    String getCaption(int formID){

        // инстанцирует форму
        return BL.baseElement.getNavigatorElement(formID).caption;
    }

    public int getDefaultForm(int classID) {

        List<NavigatorElement> relevant = BL.objectClass.findClassID(classID).relevantElements;

        if (relevant == null) return -1;

        for (NavigatorElement element : relevant) {
            if (element instanceof NavigatorForm && securityPolicy.navigator.checkPermission(element))
                return element.ID;
        }

        return -1;
    }

}

// создаются в бизнес-логике

class NavigatorElement<T extends BusinessLogics<T>> {

    int ID;
    String caption = "";

    public NavigatorElement(int iID, String icaption) { this(null, iID, icaption); }
    public NavigatorElement(NavigatorElement<T> parent, int iID, String icaption) {
        ID = iID;
        caption = icaption;

        if (parent != null)
            parent.add(this);
    }

    private NavigatorElement parent;
    NavigatorElement getParent() { return parent; }

    private List<NavigatorElement<T>> children = new ArrayList();
    Collection<NavigatorElement<T>> getChildren() { return children; }

    Collection<NavigatorElement<T>> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList(children);

        Collection<NavigatorElement<T>> result = new ArrayList();
        fillChildren(result);
        return result;
    }

    private void fillChildren(Collection<NavigatorElement<T>> result) {

        if (result.contains(this))
            return;

        result.add(this);

        for (NavigatorElement child : children)
            child.fillChildren(result);
    } 

    void add(NavigatorElement<T> child) {
        children.add(child);
        child.parent = this;
    }

    boolean hasChildren() {
        return !children.isEmpty();
    }

    NavigatorElement<T> getNavigatorElement(int elementID) {

        if (ID == elementID) return this;

        for(NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementID);
            if (element != null) return element;
        }

        return null;
    }
}

abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    List<GroupObjectImplement> Groups = new ArrayList();
    List<PropertyView> propertyViews = new ArrayList();

    Set<Filter> fixedFilters = new HashSet();
    public void addFixedFilter(Filter filter) { fixedFilters.add(filter); }

    List<RegularFilterGroup> regularFilterGroups = new ArrayList();
    public void addRegularFilterGroup(RegularFilterGroup group) { regularFilterGroups.add(group); }

    // счетчик идентификаторов
    int IDCount = 0;

    int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }

    ObjectImplement addSingleGroupObjectImplement(Class baseClass, String caption, List<Property> properties, Object... groups) {

        GroupObjectImplement groupObject = new GroupObjectImplement(IDShift(1));
        ObjectImplement object = new ObjectImplement(IDShift(1), baseClass, caption, groupObject);
        addGroup(groupObject);

        addPropertyView(properties, groups, object);

        return object;
    }

    void addGroup(GroupObjectImplement Group) {
        Groups.add(Group);
        Group.Order = Groups.size();
    }

    void addPropertyView(ObjectImplement object, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object);
    }

    void addPropertyView(ObjectImplement object1, ObjectImplement object2, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object1, object2);
    }

    private void addPropertyView(List<Property> properties, Object[] groups, ObjectImplement... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) continue;

            if (group instanceof AbstractGroup) {
                boolean upClasses = false;
                if ((i+1)<groups.length && groups[i+1] instanceof Boolean) upClasses = (Boolean)groups[i+1];
                addPropertyView(properties, (AbstractGroup)group, upClasses, objects);
            }
            else if (group instanceof LP)
                addPropertyView((LP)group, objects);
        }
    }

    void addPropertyView(List<Property> properties, Boolean upClasses, ObjectImplement... objects) {
        addPropertyView(properties, (AbstractGroup)null, upClasses, objects);
    }

    void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, ObjectImplement... objects) {
        addPropertyView(properties, group, upClasses, null, objects);
    }

    void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, GroupObjectImplement groupObject, ObjectImplement... objects) {

        // приходится делать именно так, так как важен порядок следования свойств

        for (Property property : properties) {

            if (property.getParent() == null) continue;

            if (group == null && !(property instanceof DataProperty)) continue;

            if (group != null && !group.hasChild(property)) continue;

            if (property.interfaces.size() == objects.length) {

                addPropertyView(property, upClasses, groupObject, objects);
            }
        }
    }

    <P extends PropertyInterface<P>> void addPropertyView(Property<P> property, Boolean upClasses, GroupObjectImplement groupObject, ObjectImplement... objects) {

        for (List<P> mapping : new ListPermutations<P>(property.interfaces)) {

            InterfaceClass<P> propertyInterface = new InterfaceClass();
            int interfaceCount = 0;
            for (P iface : mapping) {
                Class baseClass = objects[interfaceCount++].baseClass;
                propertyInterface.put(iface, (upClasses) ? ClassSet.getUp(baseClass)
                                                         : new ClassSet(baseClass));
            }

            if (!property.getValueClass(propertyInterface).isEmpty()) {
                addPropertyView(new LP<P,Property<P>>(property, mapping), groupObject, objects);
            }
        }
    }

    PropertyView addPropertyView(LP property, ObjectImplement... objects) {
        return addPropertyView(property, null, objects);
    }

    PropertyView addPropertyView(LP property, GroupObjectImplement groupObject, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = addPropertyObjectImplement(property, objects);
        return addPropertyView(groupObject, propertyImplement);
    }

    PropertyView addPropertyView(GroupObjectImplement groupObject, PropertyObjectImplement propertyImplement) {

        PropertyView propertyView = new PropertyView(IDShift(1),propertyImplement,(groupObject == null) ? propertyImplement.GetApplyObject() : groupObject);

        if (propertyImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (PropertyView property : propertyViews)
                if (BaseUtils.compareObjects(property.sID, propertyImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            propertyView.sID = propertyImplement.property.sID + ((foundSID) ? propertyView.ID : "");
        }
        
        propertyViews.add(propertyView);

        return propertyView;
    }

    PropertyObjectImplement addPropertyObjectImplement(LP property, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = new PropertyObjectImplement(property.property);

        ListIterator<PropertyInterface> i = property.ListInterfaces.listIterator();
        for(ObjectImplement object : objects) {
            propertyImplement.mapping.put(i.next(), object);
        }

        return propertyImplement;
    }


    PropertyView getPropertyView(PropertyObjectImplement prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }


    PropertyView getPropertyView(Property prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view.property == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }

    PropertyView getPropertyView(Property prop, GroupObjectImplement groupObject) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view.property == prop && propView.toDraw == groupObject)
                resultPropView = propView;
        }

        return resultPropView;
    }

    void addHintsNoUpdate(List<Property> properties, AbstractGroup group) {

        for (Property property : properties) {
            if (group != null && !group.hasChild(property)) continue;
            addHintsNoUpdate(property);
        }
    }

    Collection<Property> hintsNoUpdate = new HashSet();
    Collection<Property> hintsSave = new HashSet();

    void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    void addHintsSave(Property prop) {
        hintsSave.add(prop);
    }

    boolean isPrintForm;

    NavigatorForm(int iID, String caption) { this(iID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { this(null, iID, caption, iisPrintForm); }
    NavigatorForm(NavigatorElement parent, int iID, String caption) { this(parent, iID, caption, false); }
    NavigatorForm(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) { super(parent, iID, caption); isPrintForm = iisPrintForm; }

    ClientFormView richDesign;
    ClientFormView getRichDesign() { if (richDesign == null) return new DefaultClientFormView(this); else return richDesign; }

    JasperDesign reportDesign;
    JasperDesign getReportDesign() { if (reportDesign == null) return new DefaultJasperDesign(getRichDesign()); else return reportDesign; }

    ArrayList<NavigatorElement> relevantElements = new ArrayList();
    void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }



}

class ClassCache extends LinkedHashMap<Class, Integer> {

    public ClassCache() {
    }

    public ClassCache(ClassCache classCache) {
        super(classCache);
    }

    public Integer put(Class cls, Integer value) {

        if (cls == null) {
            throw new RuntimeException("Unable to put null key to cache");
        }

        if (containsKey(cls)) remove(cls);

        if (value != null)
            return super.put(cls, value);
        else
            return null;
    }

    public Integer getObject(Class cls) {

        Integer objectID = -1;
        for (Map.Entry<Class, Integer> entry : entrySet()) {
            if (entry.getKey().isParent(cls)) objectID = entry.getValue();
        }

        return objectID;
    }
}