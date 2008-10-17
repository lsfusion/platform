/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

// навигатор работает с абстрактной BL

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

        currentClass = BL.objectClass.FindClassID(classID);
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

        Map<GroupObjectImplement, GroupObjectImplement> gnvrm = new HashMap();
        Map<ObjectImplement, ObjectImplement> onvrm = new HashMap();
        Map<PropertyObjectImplement, PropertyObjectImplement> pnvrm = new HashMap();

        remoteForm.Groups = new ArrayList();
        for (GroupObjectImplement navigatorGroupObject : (List<GroupObjectImplement>)navigatorForm.Groups) {

            GroupObjectImplement groupObject = new GroupObjectImplement();

            groupObject.GID = navigatorGroupObject.GID;
            groupObject.Order = navigatorGroupObject.Order;
            for (ObjectImplement navigatorObject : navigatorGroupObject) {

                ObjectImplement object = new ObjectImplement(navigatorObject.ID, navigatorObject.BaseClass);
                object.caption = navigatorObject.caption;

                groupObject.addObject(object);
                onvrm.put(navigatorObject, object);
            }

            remoteForm.Groups.add(groupObject);
            gnvrm.put(navigatorGroupObject, groupObject);
        }

        remoteForm.Properties = new ArrayList();
        for (PropertyView navigatorProperty : (List<PropertyView>)navigatorForm.Properties) {

            PropertyObjectImplement navigatorPropObject = navigatorProperty.View;

            PropertyObjectImplement propObject = new PropertyObjectImplement(navigatorPropObject.Property);
            propObject.Mapping = new HashMap();
            for (Map.Entry<PropertyInterface, ObjectImplement> entry : navigatorPropObject.Mapping.entrySet()) {
                propObject.Mapping.put(entry.getKey(), onvrm.get(entry.getValue()));
            }

            PropertyView property = new PropertyView(navigatorProperty.ID, propObject, gnvrm.get(navigatorProperty.ToDraw));
            property.ID = navigatorProperty.ID;

            remoteForm.Properties.add(property);
            pnvrm.put(navigatorPropObject, propObject);
        }

        remoteForm.fixedFilters = new HashSet();

        for (Filter navigatorFilter : (Set<Filter>)navigatorForm.fixedFilters) {

            ValueLink value = null;

            ValueLink navigatorValue = navigatorFilter.Value;

            if (navigatorValue instanceof UserValueLink) {
                value = new UserValueLink(((UserValueLink)navigatorValue).Value);
            }

            if (navigatorValue instanceof ObjectValueLink) {
                value = new UserValueLink(onvrm.get(((ObjectValueLink)navigatorValue).Object));
            }

            if (navigatorValue instanceof PropertyValueLink) {
                value = new PropertyValueLink(pnvrm.get(((PropertyValueLink)navigatorValue).Property));
            }

            Filter filter = new Filter(pnvrm.get(navigatorFilter.Property), navigatorFilter.Compare, value);
            remoteForm.fixedFilters.add(filter);
        }

        remoteForm.filters = new HashSet(remoteForm.fixedFilters);

        remoteForm.richDesign = navigatorForm.getRichDesign();

        for (GroupObjectImplement groupObject : (List<GroupObjectImplement>)remoteForm.Groups)
            for (ObjectImplement object : groupObject) {
                int objectID = classCache.getObject(object.BaseClass);
                if (objectID != -1)
                    remoteForm.UserObjectSeeks.put(object, objectID);
            }

        return remoteForm;
    }
    
    private ClassCache classCache;

    public void addCacheObject(int classID, int value) {
        addCacheObject(BL.objectClass.FindClassID(classID), value);
    }

    public void addCacheObject(Class cls, Integer value) {
        classCache.put(cls, value);
    }

    public int getCacheObject(int classID) {
        return getCacheObject(BL.objectClass.FindClassID(classID));
    }

    public int getCacheObject(Class cls) {
        return classCache.getObject(cls);
    }

    String getCaption(int formID){

        // инстанцирует форму
        return BL.baseElement.getNavigatorElement(formID).caption;
    }

    public int getDefaultForm(int classID) {

        List<NavigatorElement> relevant = BL.objectClass.FindClassID(classID).relevantElements;

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
    List<PropertyView> Properties = new ArrayList();

    Set<Filter> fixedFilters = new HashSet();
    public void addFixedFilter(Filter filter) { fixedFilters.add(filter); }

    // счетчик идентивикаторов
    int IDCount = 0;

    int GenID(int Offs) {
        return IDCount + Offs;
    }

    int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }

    void addGroup(GroupObjectImplement Group) {
        Groups.add(Group);
        Group.Order = Groups.size();
    }

    boolean isPrintForm;

    NavigatorForm(int iID, String caption) { this(iID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { super(iID, caption); isPrintForm = iisPrintForm; }

    ClientFormView richDesign;
    ClientFormView getRichDesign() { if (richDesign == null) return new DefaultClientFormView(this); else return richDesign; } 

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
            if (entry.getKey().IsParent(cls)) objectID = entry.getValue();
        }

        return objectID;
    }
}