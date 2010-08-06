/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.view.navigator;

// навигатор работает с абстрактной BL

import platform.interop.RemoteObject;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.query.Query;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;
import platform.server.view.form.*;
import platform.server.view.form.client.RemoteFormView;
import platform.base.BaseUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.*;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteObject implements RemoteNavigatorInterface, FocusView<T>, CustomClassView, UserController {

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

    PropertyObjectInterface computer;
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
                    navigatorElements = new ArrayList(currentForm.navigatorForm.relevantElements);
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
    RemoteForm<T> currentForm;

    //используется для RelevantClassNavigator
    CustomClass currentClass;
    public boolean changeCurrentClass(int classID) {

        CustomClass changeClass = BL.baseClass.findClassID(classID);
        if (currentClass != null && currentClass.equals(changeClass)) return false;

        currentClass = changeClass;
        return true;
    }

    public void gainedFocus(RemoteForm<T> form) {
        currentForm = form;
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    private NavigatorForm<T> getNavigatorForm(int formID) {
        NavigatorForm<T> navigatorForm = (NavigatorForm) BL.baseElement.getNavigatorElement(formID);
        if(navigatorForm==null)
            throw new RuntimeException("Форма с заданным идентификатором не найдена");

         if (!securityPolicy.navigator.checkPermission(navigatorForm)) return null;
         return navigatorForm;
    }

    public String getForms(String formSet) throws RemoteException {
        return BL.getForms(formSet);
    }

    public RemoteFormInterface createForm(int formID, boolean currentSession) {

       try {
            NavigatorForm<T> navigatorForm = getNavigatorForm(formID);

            DataSession session;
            if (currentSession && currentForm != null)
                session = currentForm.session;
            else {
                session = BL.createSession(this);
                sessions.put(session, true);
            }

           RemoteForm<T> remoteForm = new RemoteForm<T>(navigatorForm, BL, session, securityPolicy, this, this, computer);

            for (GroupObjectImplement groupObject : remoteForm.groups) {
                Map<OrderView,Object> userSeeks = new HashMap<OrderView, Object>();
                for (ObjectImplement object : groupObject.objects)
                    if(object instanceof CustomObjectImplement) {
                        Integer objectID = classCache.getObject(((CustomObjectImplement)object).baseClass);
                        if (objectID != null)
                            userSeeks.put(object, objectID);
                    }
                if(!userSeeks.isEmpty())
                    remoteForm.userGroupSeeks.put(groupObject,userSeeks);
            }

            return new RemoteFormView<T,RemoteForm<T>>(remoteForm,navigatorForm.getRichDesign(),navigatorForm.getReportDesign(),exportPort);

        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(ConcreteCustomClass cls, int value) {
        classCache.put(cls, value);
    }

    public String getCaption(int formID){

        // инстанцирует форму
        return BL.baseElement.getNavigatorElement(formID).caption;
    }
}

