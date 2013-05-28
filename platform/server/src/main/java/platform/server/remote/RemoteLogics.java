package platform.server.remote;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.base.BaseUtils;
import platform.base.Subsets;
import platform.base.SystemUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.interop.RemoteLogicsInterface;
import platform.interop.event.IDaemonTask;
import platform.interop.exceptions.RemoteMessageException;
import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.UserInfo;
import platform.server.ServerLoggers;
import platform.server.classes.ValueClass;
import platform.server.data.type.TypeSerializer;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.lifecycle.LifecycleListener;
import platform.server.logics.*;
import platform.server.logics.SecurityManager;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static platform.server.logics.ServerResourceBundle.getString;

public class RemoteLogics<T extends BusinessLogics> extends ContextAwarePendingRemoteObject implements RemoteLogicsInterface, InitializingBean, LifecycleListener {
    protected final static Logger logger = ServerLoggers.remoteLogger;

    protected T businessLogics;
    protected BaseLogicsModule baseLM;

    protected NavigatorsManager navigatorsManager;

    protected RestartManager restartManager;
    
    protected platform.server.logics.SecurityManager securityManager;

    protected DBManager dbManager;

    private String displayName;
    private String name;

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = (T) businessLogics;
    }

    public void setLogicsInstance(LogicsInstance logicsInstance) {
        setContext(logicsInstance.getContext());
    }

    public void setNavigatorsManager(NavigatorsManager navigatorsManager) {
        this.navigatorsManager = navigatorsManager;
    }

    public void setRestartManager(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(navigatorsManager, "navigatorsManager must be specified");
        Assert.notNull(restartManager, "restartManager must be specified");
        Assert.notNull(securityManager, "securityManager must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
        //assert logicsInstance by checking the context
        Assert.notNull(getContext(), "logicsInstance must be specified");

        if (name == null) {
            name = businessLogics.getClass().getSimpleName();
        }
    }

    @Override
    public int getOrder() {
        return BLLOADER_ORDER - 1;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (LifecycleEvent.INIT.equals(event.getType())) {
            this.baseLM = businessLogics.LM;
        }
    }

    public RemoteNavigatorInterface createNavigator(boolean isFullClient, String login, String password, int computer, String remoteAddress, boolean forceCreateNew) {
        if (restartManager.isPendingRestart()) {
            return null;
        }

        return navigatorsManager.createNavigator(isFullClient, login, password, computer, remoteAddress, forceCreateNew);
    }

    protected DataSession createSession() throws SQLException {
        return dbManager.createSession();
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    public TimeZone getTimeZone() {
        return SystemUtils.getCurrentTimeZone();
    }

    public Integer getComputer(String strHostName) {
        return dbManager.getComputer(strHostName);
    }

    public void endSession(String clientInfo) {
        logger.debug("End user session " + clientInfo);
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) throws RemoteException {
        return businessLogics.getDaemonTasks(compId);
    }

    public ExternalScreen getExternalScreen(int screenID) {
        return businessLogics.getExternalScreen(screenID);
    }

    public ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException {
        return businessLogics.getExternalScreenParameters(screenID, computerId);
    }

    public void ping() throws RemoteException {
        //for filterIncl-alive
    }

    public String getDisplayName() throws RemoteException {
        return displayName;
    }

    public byte[] getMainIcon() throws RemoteException {
        return null;
    }

    public byte[] getLogo() throws RemoteException {
        return null;
    }

    public int generateNewID() throws RemoteException {
        return BaseLogicsModule.generateStaticNewID();
    }

    public byte[] getBaseClassByteArray() throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            baseLM.baseClass.serialize(dataStream);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getPropertyObjectsByteArray(byte[] byteClasses, boolean isCompulsory, boolean isAny) {
        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(byteClasses));

            Map<Integer, Integer> groupMap = new HashMap<Integer, Integer>();
            MExclMap<ValueClassWrapper, Integer> mClasses = MapFact.mExclMap();
            int size = inStream.readInt();
            for (int i = 0; i < size; i++) {
                Integer ID = inStream.readInt();
                ValueClass valueClass = TypeSerializer.deserializeValueClass(businessLogics, inStream);
                mClasses.exclAdd(new ValueClassWrapper(valueClass), ID);

                int groupId = inStream.readInt();
                if (groupId >= 0) {
                    groupMap.put(ID, groupId);
                }
            }

            ArrayList<Property> result = new ArrayList<Property>();
            ArrayList<ArrayList<Integer>> idResult = new ArrayList<ArrayList<Integer>>();

            addProperties(mClasses.immutable(), groupMap, result, idResult, isCompulsory, isAny);

            List<Property> newResult = BaseUtils.filterList(result, businessLogics.getProperties());

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            DataOutputStream dataStream = new DataOutputStream(outStream);

            ServerSerializationPool pool = new ServerSerializationPool();

            dataStream.writeInt(result.size());
            int num = 0;
            for (Property<?> property : newResult) {
                pool.serializeObject(dataStream, property);
                Iterator<Integer> it = idResult.get(num++).iterator();
                for (PropertyInterface propertyInterface : property.interfaces) {
                    pool.serializeObject(dataStream, propertyInterface);
                    dataStream.writeInt(it.next());
                }
            }

            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    <T extends PropertyInterface> void addProperties(ImMap<ValueClassWrapper, Integer> classes, Map<Integer, Integer> groupMap, ArrayList<Property> result, ArrayList<ArrayList<Integer>> idResult, boolean isCompulsory, boolean isAny) {
        Set<Integer> allGroups = new HashSet<Integer>(groupMap.values());
        MCol<ImSet<ValueClassWrapper>> mClassSets = ListFact.mCol();

        for (ImSet<ValueClassWrapper> classSet : new Subsets<ValueClassWrapper>(classes.keys())) {
            Set<Integer> classesGroups = new HashSet<Integer>();
            for (ValueClassWrapper wrapper : classSet) {
                int id = classes.get(wrapper);
                if (groupMap.containsKey(id)) {
                    classesGroups.add(groupMap.get(id));
                }
            }
            if ((isCompulsory && classesGroups.size() == allGroups.size()) ||
                    (!isCompulsory && classesGroups.size() > 0 || groupMap.isEmpty()) || classSet.isEmpty()) {
                mClassSets.add(classSet);
            }
        }

        for (PropertyClassImplement<T, ?> implement : baseLM.rootGroup.getProperties(mClassSets.immutableCol(), isAny)) {
            result.add(implement.property);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            for (T iface : implement.property.interfaces) {
                ids.add(classes.get(implement.mapping.get(iface)));
            }
            idResult.add(ids);
        }
    }

    public UserInfo getUserInfo(String username) throws RemoteException {
        return securityManager.getUserInfo(username, getExtraUserRoleNames(username));
    }

    @Override
    public int generateID() throws RemoteException {
        return dbManager.generateID();
    }

    protected List<String> getExtraUserRoleNames(String username) {
        return new ArrayList<String>();
    }

    protected Integer getUserByEmail(DataSession session, String email) throws SQLException {
        return (Integer) businessLogics.contactLM.contactEmail.read(session, new DataObject(email));
    }

    @Override
    public void remindPassword(String email, String localeLanguage) throws RemoteException {
        assert email != null;
        //todo: в будущем нужно поменять на проставление локали в Context
//            ServerResourceBundle.load(localeLanguage);
        try {
            DataSession session = createSession();
            try {
                Integer userId = getUserByEmail(session, email);
                if (userId == null) {
                    throw new RuntimeException(getString("mail.user.not.found") + ": " + email);
                }

                businessLogics.emailLM.emailUserPassUser.execute(session, new DataObject(userId, businessLogics.authenticationLM.customUser));
            } finally {
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error reminding password: ", e);
            throw new RemoteMessageException(getString("mail.error.sending.password.remind"), e);
        }
    }

    @Override
    public boolean checkPropertyViewPermission(String userName, String propertySID) {
        return securityManager.checkPropertyViewPermission(userName, propertySID);
    }

    @Override
    public boolean checkDefaultViewPermission(String propertySid) throws RemoteException {
        return securityManager.checkDefaultViewPermission(propertySid);
    }

    @Override
    public byte[] readFile(String sid, String... params) throws RemoteException {
        LCP<PropertyInterface> property = businessLogics.getLCP(sid);
        ImOrderSet<PropertyInterface> interfaces = property.listInterfaces;
        DataObject[] objects = new DataObject[interfaces.size()];
        byte[] fileBytes;
        try {
            DataSession session = createSession();
            ImMap<PropertyInterface, ValueClass> interfaceClasses = property.property.getInterfaceClasses(ClassType.ASIS);
            for (int i = 0; i < interfaces.size(); i++) {
                objects[i] = session.getDataObject(interfaceClasses.get(interfaces.get(i)), Integer.decode(params[i]));
            }
            fileBytes = (byte[]) property.read(session, objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileBytes;
    }

    @Override
    public String addUser(String username, String email, String password, String firstName, String lastName, String localeLanguage) throws RemoteException {
        return securityManager.addUser(username, email, password, firstName, lastName, localeLanguage);
    }
}

