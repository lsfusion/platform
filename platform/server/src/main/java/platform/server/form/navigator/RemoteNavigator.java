package platform.server.form.navigator;

// навигатор работает с абстрактной BL

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.WeakIdentityHashSet;
import platform.interop.action.ClientAction;
import platform.interop.event.IDaemonTask;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ServerResponse;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.CallbackMessage;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.RemoteObject;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.RemoteContextObject;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.instance.remote.RemotePausableInvocation;
import platform.server.form.view.FormView;
import platform.server.logics.*;
import platform.server.logics.property.*;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static platform.base.BaseUtils.nvl;

// приходится везде BusinessLogics Generics'ом гонять потому как при инстанцировании формы нужен конкретный класс

public class RemoteNavigator<T extends BusinessLogics<T>> extends RemoteContextObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, RemoteFormListener, Unreferenced {
    protected final static Logger logger = Logger.getLogger(RemoteNavigator.class);

    T BL;
    SQLSession sql;

    private final boolean isFullClient;

    private ClientCallBackController client;

    private DataObject user;

    private DataObject computer;

    private DataObject connection;

    private int updateTime;

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать

    public RemoteNavigator(T BL, boolean isFullClient, User currentUser, int computer, int port) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(port);

        this.isFullClient = isFullClient;

        this.client = new ClientCallBackController(port);
        this.BL = BL;
        this.classCache = new ClassCache();

        this.securityPolicy = currentUser.getSecurityPolicy();

        this.user = new DataObject(currentUser.ID, this.BL.LM.customUser);
        this.computer = new DataObject(computer, this.BL.LM.computer);
        this.sql = this.BL.createSQL();
    }

    WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<DataSession>();

    public boolean isFullClient() {
        return isFullClient;
    }

    public void changeCurrentUser(DataObject user) throws SQLException {
        this.user = user;
        this.securityPolicy = getUserSecurityPolicy();
        updateEnvironmentProperty((CalcProperty) BL.LM.currentUser.property, user);
    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        for (DataSession session : sessions)
            session.updateProperties(Collections.singleton(property));
    }

    public void relogin(String login) throws RemoteException {
        try {
            changeCurrentUser(getCurrentUser(login));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SecurityPolicy getUserSecurityPolicy() {
        try {
            return BL.readUser(getUserLogin(), createSession()).getSecurityPolicy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DataObject getCurrentUser(String login) {
        try {
            DataSession session = createSession();
            Integer userId = (Integer) BL.LM.loginToUser.read(session, new DataObject(login, StringClass.get(30)));
            DataObject user = session.getDataObject(userId, ObjectType.instance);
            session.close();
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void changePassword(String login, String newPassword) throws RemoteException {
        try {
            DataSession session = createSession();
            BL.LM.userPassword.change(newPassword, session, getCurrentUser(login));
            session.apply(BL);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentUserLogin() throws RemoteException {
        try {
            DataSession session = createSession();
            Integer userId = (Integer)BL.LM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(userId, ObjectType.instance);
            String userLogin = (String)BL.LM.userLogin.read(session, currentUser);
            session.close();
            return userLogin.trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserLogin() {
        try {
            DataSession session = createSession();
            String userLogin = (String) BL.LM.userLogin.read(session, user);
            session.close();
            return userLogin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clientExceptionLog(String info, String client, String message, String type, String erTrace) {
        String errorMessage = info + " в " + new SimpleDateFormat().format(Calendar.getInstance().getTime());
        System.err.println(errorMessage);
        logger.error(errorMessage);
        try {
            BL.logException(message, type, erTrace, this.user, client, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            query.properties.put("name", BL.LM.currentUserName.getExpr());
            objectStream.writeObject(BaseUtils.nvl((String) query.execute(session).singleValue().get("name"), "(без имени)").trim());

            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public void unreferenced() {
        killThreads();
    }

    @Override
    public BusinessLogics getBL() {
        return BL;
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

        public void changeCurrentUser(DataObject user) throws SQLException {
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

        public boolean isFullClient() {
            return weakThis.get().isFullClient();
        }
    }

    private DataSession createSession() throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        DataSession session = BL.createSession(sql, new WeakUserController(this), new WeakComputerController(this));
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
                return currentClass.getRelevantElements(BL.LM, securityPolicy);
        } else {
            navigatorElements = getElements(BL.LM.baseElement.getNavigatorElement(elementSID));
        }

        List<NavigatorElement> resultElements = new ArrayList();

        for (NavigatorElement element : navigatorElements)
            if (securityPolicy.navigator.checkPermission(element))
                resultElements.add(element);

        return resultElements;
    }

    List<NavigatorElement> getElements(NavigatorElement element) {

        if (element == null) element = BL.LM.baseElement;
        return new ArrayList(element.getChildren());
    }

    public byte[] getElementsByteArray(String groupSID) {

        List<NavigatorElement> listElements = getElements(groupSID);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(listElements.size());
            for (NavigatorElement element : listElements)
                element.serialize(dataStream, listElements);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    //используется для RelevantFormNavigator
    private WeakReference<FormInstance<T>> weakCurrentForm = null;

    public FormInstance<T> getCurrentForm() {
        if (weakCurrentForm != null)
            return weakCurrentForm.get();
        else
            return null;
    }

    public String getCurrentFormSID() {
        if (weakCurrentForm != null)
            return weakCurrentForm.get().entity.getSID();
        else {
            return null;
        }
    }

    @Override
    public Boolean getConfiguratorSecurityPolicy() throws RemoteException {
        return securityPolicy.configurator;
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
            DataObject connection = getConnection();

            if (connection != null) {
                DataSession session = createSession();

                Integer formId = (Integer) BL.LM.SIDToNavigatorElement.read(session, new DataObject(sid, BL.LM.navigatorElementSIDClass));
                if (formId == null) {
                    //будем считать, что к SID модифицированных форм будет добавляться что-нибудь через подчёркивание
                    int ind = sid.indexOf('_');
                    if (ind != -1) {
                        sid = sid.substring(0, ind);
                        formId = (Integer) BL.LM.SIDToNavigatorElement.read(session, new DataObject(sid, BL.LM.navigatorElementSIDClass));
                    }

                    if (formId == null) {
                        return;
                    }
                }

                DataObject formObject = new DataObject(formId, BL.LM.navigatorElement);

                int count = 1 + nvl((Integer) BL.LM.connectionFormCount.read(session, connection, formObject), 0);
                BL.LM.connectionFormCount.change(count, session, connection, formObject);

                session.apply(BL);
                session.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer getObject(CustomClass cls) {
        return getCacheObject(cls);
    }

    public void objectChanged(ConcreteCustomClass cls, int objectID) {
        addCacheObject(cls, objectID);
    }

    private FormEntity<T> getFormEntity(String formSID) {
        FormEntity<T> formEntity = (FormEntity<T>) BL.LM.baseElement.getNavigatorElement(formSID);

        if (formEntity == null) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.form.with.id.not.found") + " : " + formSID);
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private void setFormEntity(String formSID, FormEntity<T> formEntity) {
        FormEntity<T> prevEntity = (FormEntity) BL.LM.baseElement.getNavigatorElement(formSID);
        if (prevEntity == null)
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.form.with.id.not.found"));

        prevEntity.getParent().replaceChild(prevEntity, formEntity);
    }

    public String getForms(String formSet) throws RemoteException {
        return BL.getForms(formSet);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) {
        RemoteForm form = (RemoteForm) createForm(getFormEntity(formSID), isModal, interactive);
        if(initialObjects != null) {
            for (String objectSID : initialObjects.keySet()) {
                GroupObjectInstance groupObject = null;
                ObjectInstance object = null;
                for (GroupObjectInstance group : (List<GroupObjectInstance>) form.form.groups) {
                    for (ObjectInstance obj : group.objects) {
                        if (obj.getsID().equals(objectSID)) {
                            object = obj;
                            groupObject = group;
                            break;
                        }
                    }
                }
                if (object != null) {
                    groupObject.addSeek(object, new DataObject(Integer.decode(initialObjects.get(objectSID)), object.getCurrentClass()), true);
                }
            }
        }
        return form;
    }

    private Map<FormEntity, RemoteForm> openForms = new HashMap<FormEntity, RemoteForm>();
    private Map<FormEntity, RemoteForm> invalidatedForms = new HashMap<FormEntity, RemoteForm>();

    private RemoteFormInterface createForm(FormEntity<T> formEntity, boolean isModal, boolean interactive) {
        try {
            RemoteForm remoteForm = invalidatedForms.remove(formEntity);
            if (remoteForm == null) {

                remoteForm = createRemoteForm(
                        createFormInstance(formEntity, new HashMap<ObjectEntity, DataObject>(), createSession(), isModal, FormSessionScope.NEWSESSION, false, interactive)
                );
            }
            openForms.put(formEntity, remoteForm);

            return remoteForm;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException {
        FormEntity newFormEntity = FormEntity.deserialize(BL, formState);
        return createForm(newFormEntity, false, true);
    }

    public void saveForm(String formSID, byte[] formState) throws RemoteException {
        FormEntity<T> form = (FormEntity<T>) FormEntity.deserialize(BL, formState);
        setFormEntity(formSID, form);

        try {
            IOUtils.putFileBytes(new File(BL.getFormSerializationPath(form.getSID())), formState);
        } catch (IOException e) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.form.state.to.disk"), e);
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
                    throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.element.state.to.disk"), e);
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
                    throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.form.state.to.disk"), e);
                }
            }

            BL.mergeNavigatorTree(inStream);
            BL.saveNavigatorTree();
        } catch (IOException e) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.visual.tuning"), e);
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

    Integer getCacheObject(CustomClass cls) {
        return classCache.getObject(cls);
    }

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

    public synchronized void invalidate() throws SQLException {
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

        for (RemoteForm form : invalidatedForms.values()) {
            form.invalidate();
        }
    }

    public synchronized ClientCallBackController getClientCallBack() throws RemoteException {
        return client;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getUpdateTime() {
        return updateTime;
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

    public static class ClientCallBackController extends RemoteObject implements ClientCallBackInterface {
        private List<CallbackMessage> messages = new ArrayList<CallbackMessage>();
        private Boolean deniedRestart = null;

        public ClientCallBackController(int port) throws RemoteException {
            super(port);
        }

        public synchronized void disconnect() {
            addMessage(CallbackMessage.DISCONNECTED);
        }

        public synchronized void cutOff() {
            addMessage(CallbackMessage.CUT_OFF);
        }

        public synchronized void notifyServerRestart() {
            deniedRestart = false;
            addMessage(CallbackMessage.SERVER_RESTARTING);
        }

        public synchronized void notifyServerRestartCanceled() {
            deniedRestart = null;
        }

        public synchronized void denyRestart() {
            deniedRestart = true;
        }

        public synchronized boolean isRestartAllowed() {
            //если не спрашивали, либо если отказался
            return deniedRestart != null && !deniedRestart;
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

    @Override
    public boolean showDefaultForms() throws RemoteException {
        return BL.showDefaultForms(user);
    }

    @Override
    public ArrayList<String> getDefaultForms() throws RemoteException {
        return BL.getDefaultForms(user);
    }

    @Override
    public byte[] getNavigatorTree() throws RemoteException {

        Collection<NavigatorElement> listElements = BL.LM.baseElement.addSubTree(securityPolicy, new LinkedList<NavigatorElement>());

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(listElements.size());
            for (NavigatorElement element : listElements)
                element.serialize(dataStream, listElements);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public byte[] getCommonWindows() throws RemoteException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            BL.LM.relevantFormsWindow.serialize(dataStream);
            BL.LM.relevantClassFormsWindow.serialize(dataStream);
            BL.LM.logWindow.serialize(dataStream);
            BL.LM.statusWindow.serialize(dataStream);
            BL.LM.formsWindow.serialize(dataStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException {
        return  BL.getDaemonTasks(compId);
    }

    private final ExecutorService pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(this));
    private RemotePausableInvocation currentInvocation = null;

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException {
        final NavigatorElement element = BL.LM.baseElement.getNavigatorElement(navigatorActionSID);

        if (!(element instanceof NavigatorAction)) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.action.not.found"));
        }

        if (!securityPolicy.navigator.checkPermission(element)) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.not.enough.permissions"));
        }

        final ActionProperty property = ((NavigatorAction) element).getProperty();
        currentInvocation = new RemotePausableInvocation(pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                DataSession session = createSession();
                property.execute(new HashMap<ClassPropertyInterface, DataObject>(), session, null);
                session.apply(BL);
                session.close();
                assert !delayRemoteChanges; // тут не должно быть никаких delayRemote
                return new ServerResponse(delayedActions.toArray(new ClientAction[delayedActions.size()]), false);
            }
        };

        return currentInvocation.execute();
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Exception clientException) throws RemoteException {
        return currentInvocation.resumWithException(clientException);
    }

    public String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    @Override
    public Object[] requestUserInteraction(final ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean interactive) throws SQLException {
        return new FormInstance<T>(formEntity, BL,
                                   sessionScope.isNewSession() ? session.createSession() : session,
                                   securityPolicy, this, this, computer, mapObjects, isModal, sessionScope.isManageSession(),
                                   checkOnOk, interactive, null);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, exportPort, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isBusy() {
        if (!threads.isEmpty()) {
            return true;
        }

        for (RemoteForm form : openForms.values()) {
            if (!form.threads.isEmpty()) {
                return true;
            }
        }
        for (RemoteForm form : invalidatedForms.values()) {
            if (!form.threads.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void killThreads() {
        if (!threads.isEmpty()) {
            super.killThreads();
        }
        for (RemoteForm form : openForms.values()) {
            if (!form.threads.isEmpty()) {
                form.killThreads();
            }
        }
        for (RemoteForm form : invalidatedForms.values()) {
            if (!form.threads.isEmpty()) {
                form.killThreads();
            }
        }
    }

    public FormInstance getFormInstance() {
        return null;
    }
}