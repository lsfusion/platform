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

    public final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    public final static int NAVIGATORGROUP_RELEVANTCLASS = -3;

    List<NavigatorElement> GetElements(int elementID) {

        switch (elementID) {
            case (NAVIGATORGROUP_RELEVANTFORM) :
                if (currentForm == null)
                    return new ArrayList();
                else
                    return new ArrayList(((NavigatorForm)BL.baseElement.getNavigatorElement(currentForm.getID())).relevantElements);
            case (NAVIGATORGROUP_RELEVANTCLASS) :
                if (currentClass == null)
                    return new ArrayList();
                else
                    return new ArrayList(currentClass.relevantElements);
            default :
                return GetElements(BL.baseElement.getNavigatorElement(elementID));
        }
    }

    List<NavigatorElement> GetElements(NavigatorElement element) {

        if (element == null) element = BL.baseElement;

        return new ArrayList(element.childs);
    }

    public byte[] GetElementsByteArray(int groupID) {
        return ByteArraySerializer.serializeListNavigatorElement(GetElements(groupID));
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
    RemoteForm<T> CreateForm(int formID, boolean currentSession) throws SQLException {

        NavigatorForm navigatorForm = (NavigatorForm)BL.baseElement.getNavigatorElement(formID);

        DataSession session;
        if (currentSession && currentForm != null)
            session = currentForm.Session;
        else
            session = BL.createSession(Adapter);

        RemoteForm remoteForm = new RemoteForm(formID, BL, session) {

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
        FilterMapper filterMapper = new FilterMapper(objectMapper, propertyMapper);

        remoteForm.Groups = new ArrayList();
        for (GroupObjectImplement navigatorGroupObject : (List<GroupObjectImplement>)navigatorForm.Groups) {
            remoteForm.Groups.add(groupObjectMapper.doMapping(navigatorGroupObject));
        }

        remoteForm.Properties = new ArrayList();
        for (PropertyView navigatorProperty : (List<PropertyView>)navigatorForm.propertyViews) {
            remoteForm.Properties.add(new PropertyView(navigatorProperty.ID, propertyMapper.doMapping(navigatorProperty.View), groupObjectMapper.doMapping(navigatorProperty.ToDraw)));
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

        remoteForm.richDesign = navigatorForm.getRichDesign();

        remoteForm.reportDesign = navigatorForm.getReportDesign();

        for (GroupObjectImplement groupObject : (List<GroupObjectImplement>)remoteForm.Groups)
            for (ObjectImplement object : groupObject) {
                int objectID = classCache.getObject(object.BaseClass);
                if (objectID != -1)
                    remoteForm.UserObjectSeeks.put(object, objectID);
            }

        return remoteForm;
    }

    private class ObjectImplementMapper {

        private Map<ObjectImplement, ObjectImplement> mapper = new HashMap();

        ObjectImplement doMapping(ObjectImplement objKey) {

            if (mapper.containsKey(objKey)) return mapper.get(objKey);

            ObjectImplement objValue = new ObjectImplement(objKey.ID, objKey.BaseClass);
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

            PropertyObjectImplement propValue = new PropertyObjectImplement(propKey.Property);
            for (Map.Entry<? extends PropertyInterface,ObjectImplement> entry : propKey.Mapping.entrySet()) {
                propValue.Mapping.put(entry.getKey(), objectMapper.doMapping(entry.getValue()));
            }

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
            if (element instanceof NavigatorForm)
                return element.ID;
        }

        return -1;
    }

}

// создаются в бизнес-логике

class NavigatorElement<T extends BusinessLogics<T>> {

    int ID;
    String caption = "";

    public NavigatorElement(int iID, String icaption) { ID = iID; caption = icaption; }


    List<NavigatorElement<T>> childs = new ArrayList();
    void addChild(NavigatorElement<T> child) {
        childs.add(child);
    }
    public boolean allowChildren() {
        return !childs.isEmpty();
    }

    NavigatorElement<T> getNavigatorElement(int elementID) {

        if (ID == elementID) return this;

        for(NavigatorElement<T> child : childs) {
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

    Collection<Property> hintsNoUpdate = new HashSet();
    
    // счетчик идентификаторов
    int IDCount = 0;

    int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }

    void addGroup(GroupObjectImplement Group) {
        Groups.add(Group);
        Group.Order = Groups.size();
    }

    void addPropertyView(List<Property> properties, ObjectImplement... objects) {
        addPropertyView(properties, (AbstractGroup)null, objects);
    }

    void addPropertyView(List<Property> properties, AbstractGroup group, ObjectImplement... objects) {

        for (Property property : properties) {

            if (group != null && !group.hasChild(property)) continue;

            if (property.Interfaces.size() == objects.length) {

                addPropertyView(property, objects);
            }
        }
    }

    <P extends PropertyInterface<P>> void addPropertyView(Property<P> property, ObjectImplement... objects) {

        Collection<List<P>> permutations = MapBuilder.buildPermutations(property.Interfaces);

        for (List<P> mapping : permutations) {

            InterfaceClass<P> propertyInterface = new InterfaceClass();
            int interfaceCount = 0;
            for (P iface : mapping) {
                propertyInterface.put(iface, ClassSet.getUp(objects[interfaceCount++].BaseClass));
            }

            if (!property.getValueClass(propertyInterface).isEmpty()) {
                addPropertyView(new LP<P,Property<P>>(property, mapping), objects);
            }
        }
    }

    PropertyView addPropertyView(LP property, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = addPropertyObjectImplement(property, objects);
        return addPropertyView(propertyImplement);
    }

    PropertyView addPropertyView(PropertyObjectImplement propertyImplement) {

        PropertyView propertyView = new PropertyView(IDShift(1),propertyImplement,propertyImplement.GetApplyObject());
        propertyViews.add(propertyView);
        return propertyView;
    }

    PropertyObjectImplement addPropertyObjectImplement(LP property, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = new PropertyObjectImplement(property.Property);

        ListIterator<PropertyInterface> i = property.ListInterfaces.listIterator();
        for(ObjectImplement object : objects) {
            propertyImplement.Mapping.put(i.next(), object);
        }

        return propertyImplement;
    }


    PropertyView getPropertyView(PropertyObjectImplement prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.View == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }


    PropertyView getPropertyView(Property prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.View.Property == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }

    PropertyView getPropertyView(Property prop, GroupObjectImplement groupObject) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.View.Property == prop && propView.ToDraw == groupObject)
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

    void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    boolean isPrintForm;

    NavigatorForm(int iID, String caption) { this(iID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { super(iID, caption); isPrintForm = iisPrintForm; }

    ClientFormView richDesign;
    ClientFormView getRichDesign() { if (richDesign == null) return new DefaultClientFormView(this); else return richDesign; }

    JasperDesign reportDesign;
    JasperDesign getReportDesign() { if (reportDesign == null) return new DefaultJasperDesign(this); else return reportDesign; }

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