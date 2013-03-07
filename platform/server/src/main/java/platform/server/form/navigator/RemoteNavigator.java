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
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.interop.action.ClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ServerResponse;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.server.auth.SecurityPolicy;
import platform.server.auth.User;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.context.ContextAwareDaemonThreadFactory;
import platform.server.remote.ContextAwarePendingRemoteObject;
import platform.server.data.SQLSession;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.remote.RemoteForm;
import platform.server.remote.RemotePausableInvocation;
import platform.server.form.view.FormView;
import platform.server.logics.*;
import platform.server.logics.SecurityManager;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

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

public class RemoteNavigator<T extends BusinessLogics<T>> extends ContextAwarePendingRemoteObject implements RemoteNavigatorInterface, FocusListener<T>, CustomClassListener, RemoteFormListener, Unreferenced {
    protected final static Logger logger = Logger.getLogger(RemoteNavigator.class);

    SQLSession sql;

    final LogicsInstance logicsInstance;
    private final BusinessLogics businessLogics;
    private final SecurityManager securityManager;
    private final DBManager dbManager;

    // просто закэшируем, чтобы быстрее было
    SecurityPolicy securityPolicy;

    DataObject user;

    DataObject computer;

    DataObject connection;

    private int updateTime;

    private String remoteAddress;

    WeakIdentityHashSet<DataSession> sessions = new WeakIdentityHashSet<DataSession>();

    private final boolean isFullClient;

    private ClientCallBackController client;

    private final ExecutorService pausablesExecutor;
    private RemotePausableInvocation currentInvocation = null;

    private Map<FormEntity, RemoteForm> openForms = MapFact.mAddRemoveMap();
    private Map<FormEntity, RemoteForm> invalidatedForms = MapFact.mAddRemoveMap();

    // в настройку надо будет вынести : по группам, способ релевантности групп, какую релевантность отсекать
    public RemoteNavigator(LogicsInstance logicsInstance, boolean isFullClient, String remoteAddress, User currentUser, int computer, int port) throws RemoteException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        super(port);

        this.logicsInstance = logicsInstance;
        this.businessLogics = logicsInstance.getBusinessLogics();
        this.securityManager = logicsInstance.getSecurityManager();
        this.dbManager = logicsInstance.getDbManager();

        this.isFullClient = isFullClient;

        setContext(new RemoteNavigatorContext(this));

        pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(context));

        this.client = new ClientCallBackController(port);
        this.classCache = new ClassCache();

        this.securityPolicy = currentUser.getSecurityPolicy();

        this.user = new DataObject(currentUser.ID, businessLogics.LM.customUser);
        this.computer = new DataObject(computer, businessLogics.LM.computer);

        this.remoteAddress = remoteAddress;
        this.sql = dbManager.createSQL();
    }

    public boolean isFullClient() {
        return isFullClient;
    }

    public void changeCurrentUser(DataObject user) throws SQLException {
        this.user = user;
        this.securityPolicy = getUserSecurityPolicy();
        updateEnvironmentProperty((CalcProperty) businessLogics.LM.currentUser.property, user);
    }

    public void updateEnvironmentProperty(CalcProperty property, ObjectValue value) throws SQLException {
        for (DataSession session : sessions)
            session.updateProperties(SetFact.singleton(property));
    }

    public SecurityPolicy getUserSecurityPolicy() {
        try {
            return securityManager.readUser(getUserLogin(), createSession()).getSecurityPolicy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserLogin() {
        try {
            DataSession session = createSession();
            String userLogin = (String) businessLogics.LM.userLogin.read(session, user);
            session.close();
            return userLogin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    Object[] requestUserInteraction(final ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void logClientException(String info, String client, String message, String type, String erTrace) {
        String errorMessage = info + " в " + new SimpleDateFormat().format(Calendar.getInstance().getTime());
        System.err.println(errorMessage);
        logger.error(errorMessage);
        try {
            businessLogics.systemEventsLM.logException(businessLogics, message, type, erTrace, this.user, client, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public byte[] getCurrentUserInfoByteArray() {
        try {
            DataSession session = createSession();
            QueryBuilder<Object, String> query = new QueryBuilder<Object, String>(MapFact.<Object, KeyExpr>EMPTYREV());
            query.addProperty("name", businessLogics.LM.currentUserName.getExpr());
            String userName = BaseUtils.nvl((String) query.execute(session).singleValue().get("name"), "(без имени)").trim();
            session.close();

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            dataStream.writeUTF(userName);
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void unreferenced() {
        killThreads();
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

    private static class WeakUserController implements UserController { // чтобы помочь сборщику мусора и устранить цикл
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

    private static class WeakComputerController implements ComputerController { // чтобы помочь сборщику мусора и устранить цикл
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
        DataSession session = dbManager.createSession(sql, new WeakUserController(this), new WeakComputerController(this));
        sessions.add(session);
        return session;
    }

    public byte[] getElementsByteArray(String groupSID) {

        List<NavigatorElement> navigatorElements = null;
        if (groupSID.equals(RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTFORM)) {
            FormInstance<T> currentForm = getCurrentForm();
            if (currentForm != null) {
                navigatorElements = new ArrayList<NavigatorElement>(currentForm.entity.relevantElements);
            }
        } else if (groupSID.equals(RemoteNavigatorInterface.NAVIGATORGROUP_RELEVANTCLASS)) {
            if (currentClass != null) {
                navigatorElements = currentClass.getRelevantElements(businessLogics.LM, securityPolicy);
            }
        } else {
            NavigatorElement singleElement = businessLogics.LM.root.getNavigatorElement(groupSID);
            navigatorElements = singleElement == null
                                ? Collections.singletonList(singleElement)
                                : new ArrayList<NavigatorElement>(singleElement.getChildren(false));
        }

        List<NavigatorElement> resultElements = new ArrayList();
        if (navigatorElements != null) {
            for (NavigatorElement element1 : navigatorElements) {
                if (securityPolicy.navigator.checkPermission(element1)) {
                    resultElements.add(element1);
                }
            }
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(resultElements.size());
            for (NavigatorElement element : resultElements) {
                element.serialize(dataStream);
            }
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
    public boolean isConfiguratorAllowed() throws RemoteException {
        return securityPolicy.configurator != null && securityPolicy.configurator;
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

                Integer formId = (Integer) businessLogics.reflectionLM.navigatorElementSID.read(session, new DataObject(sid, businessLogics.reflectionLM.navigatorElementSIDClass));
                if (formId == null) {
                    //будем считать, что к SID модифицированных форм будет добавляться что-нибудь через подчёркивание
                    int ind = sid.indexOf('_');
                    if (ind != -1) {
                        sid = sid.substring(0, ind);
                        formId = (Integer) businessLogics.reflectionLM.navigatorElementSID.read(session, new DataObject(sid, businessLogics.reflectionLM.navigatorElementSIDClass));
                    }

                    if (formId == null) {
                        return;
                    }
                }

                DataObject formObject = new DataObject(formId, businessLogics.reflectionLM.navigatorElement);

                int count = 1 + nvl((Integer) businessLogics.systemEventsLM.connectionFormCount.read(session, connection, formObject), 0);
                businessLogics.systemEventsLM.connectionFormCount.change(count, session, connection, formObject);

                session.apply(businessLogics);
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
        FormEntity<T> formEntity = (FormEntity<T>) businessLogics.getFormEntity(formSID);

        if (formEntity == null) {
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.form.with.id.not.found") + " : " + formSID);
        }

        if (!securityPolicy.navigator.checkPermission(formEntity)) {
            return null;
        }

        return formEntity;
    }

    private void setFormEntity(String formSID, FormEntity<T> formEntity) {
        FormEntity<T> prevEntity = (FormEntity) businessLogics.LM.root.getNavigatorElement(formSID);
        if (prevEntity == null)
            throw new RuntimeException(ServerResourceBundle.getString("form.navigator.form.with.id.not.found"));

        prevEntity.getParent().replace(prevEntity, formEntity);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) {
        RemoteForm form = (RemoteForm) createForm(getFormEntity(formSID), isModal, interactive);
        if(initialObjects != null) {
            for (String objectSID : initialObjects.keySet()) {
                GroupObjectInstance groupObject = null;
                ObjectInstance object = null;
                for (GroupObjectInstance group : (List<GroupObjectInstance>) form.form.getOrderGroups()) {
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

    private RemoteFormInterface createForm(FormEntity<T> formEntity, boolean isModal, boolean interactive) {
        try {
            RemoteForm remoteForm = invalidatedForms.remove(formEntity);
            if (remoteForm == null) {

                remoteForm = context.createRemoteForm(
                        context.createFormInstance(formEntity, MapFact.<ObjectEntity, DataObject>EMPTY(), createSession(), isModal, FormSessionScope.NEWSESSION, false, false, interactive)
                );
            }
            openForms.put(formEntity, remoteForm);

            return remoteForm;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException {
        FormEntity newFormEntity = FormEntity.deserialize(businessLogics, formState);
        return createForm(newFormEntity, false, true);
    }

    public void saveForm(String formSID, byte[] formState) throws RemoteException {
        FormEntity<T> form = (FormEntity<T>) FormEntity.deserialize(businessLogics, formState);
        setFormEntity(formSID, form);

        try {
            IOUtils.putFileBytes(new File(businessLogics.getFormSerializationPath(form.getSID())), formState);
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
                    IOUtils.putFileBytes(new File(businessLogics.getElementSerializationPath(element.getSID())), data, previousBytesReaden, elementSize);
                } catch (IOException e) {
                    throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.element.state.to.disk"), e);
                }
            }

            //читаем формы
            cnt = inStream.readInt();
            for (int i = 0; i < cnt; ++i) {
                int previousBytesReaden = data.length - dataStream.available();
                FormEntity form = FormEntity.deserialize(businessLogics, inStream);
                int formSize = inStream.readInt();
                try {
                    IOUtils.putFileBytes(new File(businessLogics.getFormSerializationPath(form.getSID())), data, previousBytesReaden, formSize);
                } catch (IOException e) {
                    throw new RuntimeException(ServerResourceBundle.getString("form.navigator.error.saving.form.state.to.disk"), e);
                }
            }

            businessLogics.mergeNavigatorTree(inStream);
            businessLogics.saveNavigatorTree();
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
            new ServerSerializationPool(new ServerContext(businessLogics)).serializeObject(dataStream, getFormEntity(formSID));
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
            logicsInstance.getNavigatorsManager().removeNavigators(NavigatorFilter.single(this));
            sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public String getRemoteAddress(){
        return remoteAddress;
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

    public boolean isRestartAllowed() {
        return client.isRestartAllowed();
    }

    public synchronized void notifyServerRestart() throws RemoteException {
        client.notifyServerRestart();
    }

    public void notifyServerRestartCanceled() throws RemoteException {
        client.notifyServerRestartCanceled();
    }

    @Override
    public boolean showDefaultForms() throws RemoteException {
        return securityManager.showDefaultForms(user);
    }

    @Override
    public List<String> getDefaultForms() throws RemoteException {
        return securityManager.getDefaultForms(user);
    }

    @Override
    public byte[] getNavigatorTree() throws RemoteException {

        List<NavigatorElement<T>> elements = businessLogics.LM.root.getChildrenNonUnique(securityPolicy);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(elements.size());
            for (NavigatorElement element : elements) {
                element.serialize(dataStream);
            }

            for (NavigatorElement<T> element : elements) {
                int childrenCount = 0;
                for (NavigatorElement<T> child : element.getChildren(false)) {
                    if (elements.contains(child)) {
                        childrenCount++;
                    }
                }

                dataStream.writeInt(childrenCount);
                for (NavigatorElement<T> child : element.getChildren(false)) {
                    if (elements.contains(child)) {
                        dataStream.writeUTF(child.getSID());
                    }
                }
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public byte[] getCommonWindows() throws RemoteException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            businessLogics.LM.windows.relevantForms.serialize(dataStream);
            businessLogics.LM.windows.relevantClassForms.serialize(dataStream);
            businessLogics.LM.windows.log.serialize(dataStream);
            businessLogics.LM.windows.status.serialize(dataStream);
            businessLogics.LM.windows.forms.serialize(dataStream);
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException {
        final NavigatorElement element = businessLogics.LM.root.getNavigatorElement(navigatorActionSID);

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
                property.execute(MapFact.<ClassPropertyInterface, DataObject>EMPTY(), session, null);
                session.apply(businessLogics);
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
        return currentInvocation.resumeWithException(clientException);
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
}