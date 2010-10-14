/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.form.navigator;

// навигатор работает с абстрактной BL

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.RemoteObject;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.form.entity.FormEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.listener.CurrentClassListener;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, UserController, CurrentClassListener {

    T BL;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(T iBL, User currentUser, int computer, int port) throws RemoteException {
        super(port);

        BL = iBL;
        classCache = new ClassCache();

        securityPolicy = BL.policyManager.getSecurityPolicy(currentUser);

        user = new DataObject(currentUser.ID, BL.customUser);
        this.computer = new DataObject(computer, BL.computer);
    }

    private DataObject user;

    public DataObject getCurrentUser() {
        return user;
    }

    WeakHashMap<DataSession, Object> sessions = new WeakHashMap<DataSession, Object>();

    public void changeCurrentUser(DataObject user) {
        this.user = user;

        Modifier<? extends Changes> userModifier = new PropertyChangesModifier(Property.defaultModifier, new PropertyChanges(
                BL.currentUser.property,new PropertyChange<PropertyInterface>(new HashMap<PropertyInterface, KeyExpr>(), user.getExpr(), Where.TRUE)));
        for(DataSession session : sessions.keySet())
            session.updateProperties(userModifier);
    }

    public void relogin(String login) throws RemoteException {
        try {
            DataSession session = BL.createSession(this);
            Integer userId = (Integer) BL.loginToUser.read(session, new DataObject(login, StringClass.get(30)));
            DataObject user =  session.getDataObject(userId, ObjectType.instance);
            changeCurrentUser(user);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    

    PropertyObjectInterfaceInstance computer;
    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    public byte[] getCurrentUserInfoByteArray() {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            DataSession session = BL.createSession(this);

            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            Query<Object,String> query = new Query<Object,String>(new HashMap<Object, KeyExpr>());
            query.properties.put("name", BL.currentUserName.getExpr(session.modifier));
            objectStream.writeObject(BaseUtils.nvl((String)query.execute(session).singleValue().get("name"),"(без имени)").trim());

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                    navigatorElements = new ArrayList(currentForm.entity.relevantElements);
                break;
            case (RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS) :
                if (currentClass == null)
                    navigatorElements = new ArrayList();
                else
                    return currentClass.getRelevantElements(BL, securityPolicy);
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
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    //используется для RelevantFormNavigator
    FormInstance<T> currentForm;

    //используется для RelevantClassNavigator
    CustomClass currentClass;
    public boolean changeCurrentClass(ConcreteCustomClass customClass) {
        if (currentClass!=null && currentClass.equals(customClass)) return false;

        currentClass = customClass;
        return true;
    }

    public void gainedFocus(FormInstance<T> form) {
        currentForm = form;
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    private FormEntity<T> getFormEntity(int formID) {
        FormEntity<T> formEntity = (FormEntity) BL.baseElement.getNavigatorElement(formID);
        if(formEntity ==null)
            throw new RuntimeException("Форма с заданным идентификатором не найдена");

         if (!securityPolicy.navigator.checkPermission(formEntity)) return null;
         return formEntity;
    }

    private void setFormEntity(int formID, FormEntity<T> formEntity) {
        FormEntity<T> prevEntity = (FormEntity) BL.baseElement.getNavigatorElement(formID);
        if(prevEntity ==null)
            throw new RuntimeException("Форма с заданным идентификатором не найдена");

        prevEntity.getParent().replaceChild(prevEntity, formEntity);
    }

    public String getForms(String formSet) throws RemoteException {
        return BL.getForms(formSet);
    }

    public RemoteFormInterface createForm(int formID, boolean currentSession) {
        return createForm(getFormEntity(formID), currentSession);
    }

    public RemoteFormInterface createForm(FormEntity<T> formEntity, boolean currentSession) {

       try {
            DataSession session;
            if (currentSession && currentForm != null)
                session = currentForm.session;
            else {
                session = BL.createSession(this);
                sessions.put(session, true);
            }

           FormInstance<T> formInstance = new FormInstance<T>(formEntity, BL, session, securityPolicy, this, this, computer);

            for (GroupObjectInstance groupObject : formInstance.groups) {
                Map<OrderInstance,Object> userSeeks = new HashMap<OrderInstance, Object>();
                for (ObjectInstance object : groupObject.objects)
                    if(object instanceof CustomObjectInstance) {
                        Integer objectID = classCache.getObject(((CustomObjectInstance)object).baseClass);
                        if (objectID != null)
                            userSeeks.put(object, objectID);
                    }
                if(!userSeeks.isEmpty())
                    formInstance.userGroupSeeks.put(groupObject,userSeeks);
            }

            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort,this);

        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(byte[] formState) throws RemoteException {
        FormEntity newFormEntity = FormEntity.deserialize(BL, formState);
        return createForm(newFormEntity, false);
    }

    public void saveForm(int formID, byte[] formState) throws RemoteException {
        setFormEntity(formID, (FormEntity<T>)FormEntity.deserialize(BL, formState));

        try {
            IOUtils.putFileBytes(new File("conf/forms/form" + formID), formState);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении состояния формы на диск", e);
        }
    }

    public byte[] getRichDesignByteArray(int formID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            new ServerSerializationPool().serializeObject(dataStream, getFormEntity(formID).richDesign, SerializationType.VISUAL_SETUP);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getFormEntityByteArray(int formID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            new ServerSerializationPool().serializeObject(dataStream, getFormEntity(formID));
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(ConcreteCustomClass cls, int value) {
        classCache.put(cls, value);
    }
}

