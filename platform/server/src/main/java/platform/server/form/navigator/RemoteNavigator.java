/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.server.form.navigator;

// навигатор работает с абстрактной BL

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.WeakIdentityHashSet;
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
import platform.server.data.SQLSession;
import platform.server.form.entity.FormEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.listener.CurrentClassListener;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, CurrentClassListener {

    T BL;
    SQLSession sql;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(T BL, User currentUser, int computer, int port) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(port);

        this.BL = BL;
        classCache = new ClassCache();

        securityPolicy = this.BL.policyManager.getSecurityPolicy(currentUser);

        user = new DataObject(currentUser.ID, this.BL.customUser);
        this.computer = new DataObject(computer, this.BL.computer);
        this.sql = this.BL.createSQL();
    }

    private DataObject user;

    WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<DataSession>();
    public void changeCurrentUser(DataObject user) {
        this.user = user;

        Modifier<? extends Changes> userModifier = new PropertyChangesModifier(Property.defaultModifier, new PropertyChanges(
                BL.currentUser.property, new PropertyChange<PropertyInterface>(new HashMap<PropertyInterface, KeyExpr>(), user.getExpr(), Where.TRUE)));
        for (DataSession session : sessions)
            session.updateProperties(userModifier);
    }

    public void relogin(String login) throws RemoteException {
        try {
            DataSession session = createSession();
            Integer userId = (Integer) BL.loginToUser.read(session, new DataObject(login, StringClass.get(30)));
            DataObject user = session.getDataObject(userId, ObjectType.instance);
            changeCurrentUser(user);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clientExceptionLog(String info) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat();
        System.err.println(info + " в " +sdf.format(cal.getTime()));
    }

    PropertyObjectInterfaceInstance computer;
    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    public byte[] getCurrentUserInfoByteArray() {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            DataSession session = createSession();

            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            Query<Object, String> query = new Query<Object, String>(new HashMap<Object, KeyExpr>());
            query.properties.put("name", BL.currentUserName.getExpr());
            objectStream.writeObject(BaseUtils.nvl((String) query.execute(session.sql, session.env).singleValue().get("name"), "(без имени)").trim());

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    private static class WeakUserController implements UserController { // чтобы помочь сборщику мусора и устранить цикд
        WeakReference<RemoteNavigator> weakThis;

        private WeakUserController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<RemoteNavigator>(navigator);
        }

        public void changeCurrentUser(DataObject user) {
            weakThis.get().changeCurrentUser(user);
        }

        public DataObject getCurrentUser() {
            return weakThis.get().user;
        }
    }

    private static class WeakComputerController implements ComputerController { // чтобы помочь сборщику мусора и устранить цикд
        WeakReference<RemoteNavigator> weakThis;

        private WeakComputerController(RemoteNavigator navigator) {
            this.weakThis = new WeakReference<RemoteNavigator>(navigator);
        }

        public DataObject getCurrentComputer() {
            return weakThis.get().computer.getDataObject();
        }
    }

    private DataSession createSession() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        DataSession session = BL.createSession(sql, new WeakUserController(this) , new WeakComputerController(this));
        sessions.add(session);
        return session;
    }

    List<NavigatorElement> getElements(int elementID) {

        List<NavigatorElement> navigatorElements;
        switch (elementID) {
            case (RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM):
                FormInstance<T> currentForm = getCurrentForm();
                if (currentForm == null)
                    navigatorElements = new ArrayList<NavigatorElement>();
                else
                    navigatorElements = new ArrayList<NavigatorElement>(currentForm.entity.relevantElements);
                break;
            case (RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS):
                if (currentClass == null)
                    navigatorElements = new ArrayList();
                else
                    return currentClass.getRelevantElements(BL, securityPolicy);
                break;
            default:
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
    private WeakReference<FormInstance<T>> weakCurrentForm = null;
    public FormInstance<T> getCurrentForm() {
        if(weakCurrentForm!=null)
            return weakCurrentForm.get();
        else
            return null;
    }

    public void gainedFocus(FormInstance<T> form) {
        weakCurrentForm = new WeakReference<FormInstance<T>>(form);
    }
    
    //используется для RelevantClassNavigator
    CustomClass currentClass;

    public boolean changeCurrentClass(ConcreteCustomClass customClass) {
        if (currentClass != null && currentClass.equals(customClass)) return false;

        currentClass = customClass;
        return true;
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    private FormEntity<T> getFormEntity(int formID) {
        FormEntity<T> formEntity = (FormEntity<T>) BL.baseElement.getNavigatorElement(formID);

        if (formEntity == null) {
            throw new RuntimeException("Форма с заданным идентификатором не найдена");
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private void setFormEntity(int formID, FormEntity<T> formEntity) {
        FormEntity<T> prevEntity = (FormEntity) BL.baseElement.getNavigatorElement(formID);
        if (prevEntity == null)
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
            FormInstance<T> currentForm = getCurrentForm();
            if (currentSession && currentForm != null)
                session = currentForm.session;
            else
                session = createSession();

            FormInstance<T> formInstance = new FormInstance<T>(formEntity, BL, session, securityPolicy, this, this, computer);

            for (GroupObjectInstance groupObject : formInstance.groups) {
                Map<OrderInstance, ObjectValue> userSeeks = new HashMap<OrderInstance, ObjectValue>();
                for (ObjectInstance object : groupObject.objects)
                    if (object instanceof CustomObjectInstance) {
                        Integer objectID = classCache.getObject(((CustomObjectInstance) object).baseClass);
                        if (objectID != null)
                            userSeeks.put(object, session.getDataObject(objectID, ObjectType.instance));
                    }
                if (!userSeeks.isEmpty())
                    groupObject.seek(userSeeks, false);
            }

            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, this);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(byte[] formState) throws RemoteException {
        FormEntity newFormEntity = FormEntity.deserialize(BL, formState);
        return createForm(newFormEntity, false);
    }

    public void saveForm(int formID, byte[] formState) throws RemoteException {
        FormEntity<T> form = (FormEntity<T>) FormEntity.deserialize(BL, formState);
        setFormEntity(formID, form);

        try {
            IOUtils.putFileBytes(new File(BL.getFormSerializationPath(form.getSID())), formState);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении состояния формы на диск", e);
        }
    }

    public void saveVisualSetup(byte[] data) throws RemoteException {
        ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
        DataInputStream inStream = new DataInputStream(dataStream);
        try {
            //читаем элементы
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; ++i) {
                int previousBytesReaden = data.length - dataStream.available();
                NavigatorElement element = NavigatorElement.deserialize(inStream);
                int elementSize = inStream.readInt();
                try {
                    IOUtils.putFileBytes(new File(BL.getElementSerializationPath(element.getSID())), data, previousBytesReaden, elementSize);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при сохранении состояния элемента на диск", e);
                }
            }

            //читаем формы
            cnt = inStream.readInt();
            for (int i = 0; i < cnt; ++i) {
                int previousBytesReaden = data.length - dataStream.available();
                FormEntity form = FormEntity.deserialize(BL, inStream);
                int formSize = inStream.readInt();
                try {
                    IOUtils.putFileBytes(new File(BL.getFormSerializationPath(form.getSID())), data, previousBytesReaden, formSize);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при сохранении состояния формы на диск", e);
                }
            }

            BL.mergeNavigatorTree(inStream);
            BL.saveNavigatorTree();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении визуальной настройки", e);
        }
    }

    public byte[] getRichDesignByteArray(int formID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            FormView view = getFormEntity(formID).getRichDesign();
            new ServerSerializationPool(new ServerContext(view)).serializeObject(dataStream, view, SerializationType.VISUAL_SETUP);
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

    public void close() throws SQLException {
        sql.close();
    }
}


