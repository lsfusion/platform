package platform.server.form.navigator;

// навигатор работает с абстрактной BL

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.WeakIdentityHashSet;
import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.CallbackMessage;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.RemoteObject;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NavigatorFilter;
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

import static platform.base.BaseUtils.nvl;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, RemoteFormListener {
    protected final static Logger logger = Logger.getLogger(RemoteNavigator.class);

    T BL;
    SQLSession sql;

    private ClientCallBackController client;

    private DataObject user;

    private DataObject computer;

    private DataObject connection;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(T BL, User currentUser, int computer, int port) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(port);

        client = new ClientCallBackController(port);
        this.BL = BL;
        classCache = new ClassCache();

        securityPolicy = this.BL.policyManager.getSecurityPolicy(currentUser);

        user = new DataObject(currentUser.ID, this.BL.customUser);
        this.computer = new DataObject(computer, this.BL.computer);
        this.sql = this.BL.createSQL();
    }

    WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<DataSession>();
    public void changeCurrentUser(DataObject user) {
        this.user = user;

        updateEnvironmentProperty(BL.currentUser.property, user);
    }

    public void updateEnvironmentProperty(Property property, ObjectValue value) {
        Modifier<? extends Changes> userModifier = new PropertyChangesModifier(Property.defaultModifier, new PropertyChanges(
                property, new PropertyChange<PropertyInterface>(new HashMap<PropertyInterface, KeyExpr>(), value.getExpr(), Where.TRUE)));
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

    @Aspect
    private static class RemoteNavigatorUsageAspect {
        @Around("execution(* platform.interop.navigator.RemoteNavigatorInterface.*(..)) && target(remoteNavigator)")
        public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, RemoteNavigator remoteNavigator) throws Throwable {
            remoteNavigator.updateLastUsedTime();
            return thisJoinPoint.proceed();
        }
    }

    private long lastUsedTime;
    public void updateLastUsedTime() {
        //забиваем на синхронизацию, потому что для времени использования совсем неактуально
        //пусть потоки меняют как хотят
        lastUsedTime = System.currentTimeMillis();
    }

    public long getLastUsedTime() {
        return lastUsedTime;
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

    List<NavigatorElement> getElements(String elementSID) {

        List<NavigatorElement> navigatorElements;
        if (elementSID.equals(RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM)) {
            FormInstance<T> currentForm = getCurrentForm();
            if (currentForm == null)
                navigatorElements = new ArrayList<NavigatorElement>();
            else
                navigatorElements = new ArrayList<NavigatorElement>(currentForm.entity.relevantElements);
        } else if (elementSID.equals(RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS)) {
            if (currentClass == null)
                navigatorElements = new ArrayList();
            else
                return currentClass.getRelevantElements(BL, securityPolicy);
        } else {
            navigatorElements = getElements(BL.baseElement.getNavigatorElement(elementSID));
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

    public byte[] getElementsByteArray(String groupSID) {

        List<NavigatorElement> listElements = getElements(groupSID);

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

    public boolean currentClassChanged(ConcreteCustomClass customClass) {
        if (currentClass != null && currentClass.equals(customClass)) return false;

        currentClass = customClass;
        return true;
    }

    @Override
    public void formCreated(RemoteForm form) {
        updateOpenFormCount(form.getSID());
    }

    private void updateOpenFormCount(String sid) {
        try {
            DataSession session = createSession();

            Integer formId = (Integer) BL.SIDToNavigatorElement.read(session, new DataObject(sid, BL.formSIDValueClass));
            if (formId == null) {
                //будем считать, что к SID модифицированных форм будет добавляться что-нибудь через подчёркивание
                int ind = sid.indexOf('_');
                if (ind != -1) {
                    sid = sid.substring(0, ind);
                    formId = (Integer) BL.SIDToNavigatorElement.read(session, new DataObject(sid, BL.formSIDValueClass));
                }

                if (formId == null) {
                    return;
                }
            }

            DataObject formObject = new DataObject(formId, BL.navigatorElement);

            int count = 1 + nvl((Integer) BL.connectionFormCount.read(session, getConnection(), formObject), 0);
            BL.connectionFormCount.execute(count, session, getConnection(), formObject);

            session.apply(BL);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    private FormEntity<T> getFormEntity(String formSID) {
        FormEntity<T> formEntity = (FormEntity<T>) BL.baseElement.getNavigatorElement(formSID);

        if (formEntity == null) {
            throw new RuntimeException("Форма с заданным идентификатором не найдена");
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private void setFormEntity(String formSID, FormEntity<T> formEntity) {
        FormEntity<T> prevEntity = (FormEntity) BL.baseElement.getNavigatorElement(formSID);
        if (prevEntity == null)
            throw new RuntimeException("Форма с заданным идентификатором не найдена");

        prevEntity.getParent().replaceChild(prevEntity, formEntity);
    }

    public String getForms(String formSet) throws RemoteException {
        return BL.getForms(formSet);
    }

    public RemoteFormInterface createForm(String formSID, boolean currentSession) {
        return createForm(getFormEntity(formSID), currentSession);
    }

    private Map<FormEntity, RemoteForm> openForms = new HashMap<FormEntity, RemoteForm>();
    private Map<FormEntity, RemoteForm> invalidatedForms = new HashMap<FormEntity, RemoteForm>();
    public RemoteFormInterface createForm(FormEntity<T> formEntity, boolean currentSession) {
        RemoteForm remoteForm = invalidatedForms.remove(formEntity);
        if (remoteForm != null) {
            remoteForm.form.fullRefresh();
        } else {
            try {
                DataSession session;
                FormInstance<T> currentForm = getCurrentForm();
                if (currentSession && currentForm != null)
                    session = currentForm.session;
                else
                    session = createSession();

                Map<ObjectEntity, ObjectValue> cacheSeeks = new HashMap<ObjectEntity, ObjectValue>();
                for (GroupObjectEntity groupObject : formEntity.groups) {
                    for (ObjectEntity object : groupObject.objects)
                        if (object.baseClass instanceof CustomClass) {
                            Integer objectID = classCache.getObject((CustomClass) object.baseClass);
                            if (objectID != null)
                                cacheSeeks.put(object, session.getDataObject(objectID, ObjectType.instance));
                        }
                }

                FormInstance<T> formInstance = new FormInstance<T>(formEntity, BL, session, securityPolicy, this, this, computer, cacheSeeks);

                remoteForm = new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        openForms.put(formEntity, remoteForm);

        return remoteForm;
    }

    public RemoteFormInterface createForm(byte[] formState) throws RemoteException {
        FormEntity newFormEntity = FormEntity.deserialize(BL, formState);
        return createForm(newFormEntity, false);
    }

    public void saveForm(String formSID, byte[] formState) throws RemoteException {
        FormEntity<T> form = (FormEntity<T>) FormEntity.deserialize(BL, formState);
        setFormEntity(formSID, form);

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

    public byte[] getRichDesignByteArray(String formSID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            FormView view = getFormEntity(formSID).getRichDesign();
            new ServerSerializationPool(new ServerContext(view)).serializeObject(dataStream, view, SerializationType.VISUAL_SETUP);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getFormEntityByteArray(String formSID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            new ServerSerializationPool().serializeObject(dataStream, getFormEntity(formSID));
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClassCache classCache;

    public void addCacheObject(ConcreteCustomClass cls, int value) {
        classCache.put(cls, value);
    }

    public DataObject getUser() {
        return user;
    }

    public DataObject getComputer() {
        return computer;
    }

    public DataObject getConnection() {
        return connection;
    }

    public void setConnection(DataObject connection) {
        this.connection = connection;
    }

    public void close() throws RemoteException {
        try {
            BL.removeNavigators(NavigatorFilter.single(this));
            sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void invalidate() {
        if (client != null) {
            client.disconnect();
        }

        try {
            client = new ClientCallBackController(getExportPort());
        } catch (RemoteException ignore) {
            client = null;
        }

//        invalidatedForms.clear();

        invalidatedForms.putAll(openForms);

        openForms.clear();
    }

    public synchronized ClientCallBackController getClientCallBack() throws RemoteException {
        return client;
    }

    public boolean isRestartAllowed() {
        return client.isRestartAllowed();
    }

    public synchronized void notifyServerRestart() throws RemoteException {
        client.notifyServerRestart();
    }

    public void notifyServerRestartCanceled() throws RemoteException {
        client.notifyServerRestartCanceled();
    }

    private static class ClientCallBackController extends RemoteObject implements ClientCallBackInterface {
        private List<CallbackMessage> messages = new ArrayList<CallbackMessage>();
        private Boolean deniedRestart = null;

        public ClientCallBackController(int port) throws RemoteException {
            super(port);
        }

        public synchronized void disconnect() {
            addMessage(CallbackMessage.DISCONNECTED);
        }

        public synchronized void notifyServerRestart() {
            deniedRestart = false;
            addMessage(CallbackMessage.SERVER_RESTARTED);
        }

        public synchronized void notifyServerRestartCanceled() {
            deniedRestart = null;
        }

        public synchronized void denyRestart() {
            deniedRestart = true;
        }

        public synchronized boolean isRestartAllowed() {
            //если не спрашивали, либо если отказался
            return deniedRestart!=null && !deniedRestart;
        }

        public synchronized void addMessage(CallbackMessage message) {
            messages.add(message);
        }

        public synchronized List<CallbackMessage> pullMessages() {
            ArrayList result = new ArrayList(messages);
            messages.clear();
            return result.isEmpty() ? null : result;
        }
    }
}