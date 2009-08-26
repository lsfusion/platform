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
import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.session.DataSession;
import platform.server.view.form.*;
import platform.server.view.form.client.RemoteFormView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteObject implements RemoteNavigatorInterface {

    DataAdapter adapter;
    T BL;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(DataAdapter iAdapter,T iBL,User iCurrentUser,int port) throws RemoteException {
        super(port);

        adapter = iAdapter;
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
                    navigatorElements = new ArrayList(((NavigatorForm)BL.baseElement.getNavigatorElement(currentForm.ID)).relevantElements);
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
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    //используется для RelevantFormNavigator
    RemoteForm currentForm;

    //используется для RelevantClassNavigator
    CustomClass currentClass;
    public boolean changeCurrentClass(int classID) {

        if (currentClass != null && currentClass.ID == classID) return false;

        currentClass = BL.baseClass.findClassID(classID);
        return true;
    }

//    RemoteForm<T> lastOpenedForm;
    public RemoteFormInterface createForm(int formID, boolean currentSession) {

       try {

            NavigatorForm navigatorForm = (NavigatorForm)BL.baseElement.getNavigatorElement(formID);

            if (!securityPolicy.navigator.checkPermission(navigatorForm)) return null;

            DataSession session;
            if (currentSession && currentForm != null)
                session = currentForm.session;
            else
                session = BL.createSession(adapter);

            RemoteForm<?> remoteForm = new RemoteForm<T>(formID, BL, session, securityPolicy, navigatorForm,
                    new CustomClassView() {
                        public void objectChanged(ConcreteCustomClass cls, int objectID) {
                            addCacheObject(cls, objectID);
                        }},
                    new FocusView<T>() {
                        public void gainedFocus(RemoteForm<T> form) {
                            currentForm = form;
                        }
                    });

            for (GroupObjectImplement groupObject : remoteForm.groups) {
                Map<OrderView,Object> userSeeks = new HashMap<OrderView, Object>();
                for (ObjectImplement object : groupObject.objects)
                    if(object instanceof CustomObjectImplement) {
                        int objectID = classCache.getObject(((CustomObjectImplement)object).baseClass);
                        if (objectID != -1)
                            userSeeks.put(object, objectID);
                    }
                if(!userSeeks.isEmpty())
                    remoteForm.userGroupSeeks.put(groupObject,userSeeks);
            }

            return new RemoteFormView(remoteForm,navigatorForm.getRichDesign(),navigatorForm.getReportDesign(),exportPort);

        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(int classID, int value) {
        addCacheObject(BL.baseClass.findConcreteClassID(classID), value);
    }

    public void addCacheObject(ConcreteCustomClass cls, int value) {
        classCache.put(cls, value);
    }

    public int getCacheObject(int classID) {
        return getCacheObject(BL.baseClass.findClassID(classID));
    }

    public int getCacheObject(CustomClass cls) {
        return classCache.getObject(cls);
    }

    public String getCaption(int formID){

        // инстанцирует форму
        return BL.baseElement.getNavigatorElement(formID).caption;
    }

    public int getDefaultForm(int classID) {

        List<NavigatorElement> relevant = BL.baseClass.findClassID(classID).relevantElements;

        if (relevant == null) return -1;

        for (NavigatorElement element : relevant) {
            if (element instanceof NavigatorForm && securityPolicy.navigator.checkPermission(element))
                return element.ID;
        }

        return -1;
    }

}

