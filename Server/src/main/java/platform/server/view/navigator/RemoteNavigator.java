/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.navigator;

// навигатор работает с абстрактной BL

import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.navigator.RemoteNavigatorImplement;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.auth.SecurityPolicy;
import platform.server.logics.auth.User;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.session.DataSession;
import platform.server.view.form.*;
import platform.server.view.form.client.RemoteFormView;

import java.sql.SQLException;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;

import net.sf.jasperreports.engine.JRException;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteNavigatorImplement implements RemoteNavigatorInterface {

    DataAdapter Adapter;
    T BL;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(DataAdapter iAdapter,T iBL,User iCurrentUser) throws RemoteException {
        Adapter = iAdapter;
        BL = iBL;
        classCache = new ClassCache();

        currentUser = iCurrentUser;
        securityPolicy = BL.authPolicy.getSecurityPolicy(currentUser);
    }

    User currentUser;
    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    public byte[] getCurrentUserInfoByteArray() {

        if (currentUser == null) return new byte[] {};

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(currentUser.userInfo);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception : "+e.getMessage());
        }

        return outStream.toByteArray();
    }

    List<NavigatorElement> getElements(int elementID) {

        List<NavigatorElement> navigatorElements;
        switch (elementID) {
            case (RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM) :
                if (currentForm == null)
                    navigatorElements = new ArrayList();
                else
                    navigatorElements = new ArrayList(((NavigatorForm)BL.baseElement.getNavigatorElement(currentForm.getID())).relevantElements);
                break;
            case (RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS) :
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
        
        List<NavigatorElement> listElements = getElements(groupID);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(listElements.size());
            for (NavigatorElement element : listElements)
                element.serialize(dataStream);
        } catch (IOException e) {
            throw new RuntimeException("IO Exception : "+e.getMessage());
        }

        return outStream.toByteArray();
    }

    //используется для RelevantFormNavigator
    RemoteForm<T> currentForm;

    //используется для RelevantClassNavigator
    RemoteClass currentClass;
    public boolean changeCurrentClass(int classID) {

        if (currentClass != null && currentClass.ID == classID) return false;

        currentClass = BL.objectClass.findClassID(classID);
        return true;
    }

//    RemoteForm<T> lastOpenedForm;
    public RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException {

        try {
            NavigatorForm navigatorForm = (NavigatorForm)BL.baseElement.getNavigatorElement(formID);

            if (!securityPolicy.navigator.checkPermission(navigatorForm)) return null;

            DataSession session;
            if (currentSession && currentForm != null)
                session = currentForm.session;
            else
                session = BL.createSession(Adapter);

            RemoteForm remoteForm = new RemoteForm(formID, BL, session, securityPolicy) {

                    protected void objectChanged(RemoteClass cls, Integer objectID) {
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

            for (GroupObjectImplement groupObject : (List<GroupObjectImplement>)remoteForm.groups)
                for (ObjectImplement object : groupObject) {
                    int objectID = classCache.getObject(object.baseClass);
                    if (objectID != -1)
                        remoteForm.userObjectSeeks.put(object, objectID);
                }

            return new RemoteFormView(remoteForm,navigatorForm.getRichDesign(),navigatorForm.getReportDesign());
        } catch (Exception e) {
            throw new RemoteException("Exception : "+e.getMessage());
        }
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
            for (Map.Entry<? extends PropertyInterface, ObjectImplement> entry : propKey.mapping.entrySet()) {
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

            ValueLink navigatorValue = filterKey.value;

            if (navigatorValue instanceof UserValueLink) {
                value = new UserValueLink(((UserValueLink)navigatorValue).value);
            }

            if (navigatorValue instanceof ObjectValueLink) {
                value = new ObjectValueLink(objectMapper.doMapping(((ObjectValueLink)navigatorValue).object));
            }

            if (navigatorValue instanceof PropertyValueLink) {
                value = new PropertyValueLink(propertyMapper.doMapping(((PropertyValueLink)navigatorValue).property));
            }

            return new Filter(propertyMapper.doMapping(filterKey.property), filterKey.compare, value);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(int classID, int value) {
        addCacheObject(BL.objectClass.findClassID(classID), value);
    }

    public void addCacheObject(RemoteClass cls, Integer value) {
        classCache.put(cls, value);
    }

    public int getCacheObject(int classID) {
        return getCacheObject(BL.objectClass.findClassID(classID));
    }

    public int getCacheObject(RemoteClass cls) {
        return classCache.getObject(cls);
    }

    public String getCaption(int formID){

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

