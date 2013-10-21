package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ERunnable;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Order;
import lsfusion.interop.Scroll;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ProcessFormChangesClientAction;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ServerResponse;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ContextAwareDaemonThreadFactory;
import lsfusion.server.form.instance.*;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.form.instance.listener.RemoteFormListener;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.serialization.SerializationType;
import lsfusion.server.serialization.ServerContext;
import lsfusion.server.serialization.ServerSerializationPool;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static lsfusion.base.BaseUtils.deserializeObject;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>, F extends FormInstance<T>> extends ContextAwarePendingRemoteObject implements RemoteFormInterface {
    private final static Logger logger = ServerLoggers.remoteLogger;

    public final F form;
    private final FormView richDesign;
    public final FormReportManager<T, F> reportManager;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    private final ExecutorService pausablesExecutor;
    private final AtomicInteger numberOfFormChangesRequests = new AtomicInteger();
    private final AtomicBoolean isEditing = new AtomicBoolean(false);
    private final SequentialRequestLock requestLock;
    private RemotePausableInvocation currentInvocation = null;

    public RemoteForm(F form, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        super(port);

        setContext(new RemoteFormContext(this));

        this.form = form;
        this.richDesign = form.entity.getRichDesign();
        this.reportManager = new FormReportManager(form);
        this.requestLock = new SequentialRequestLock();

        pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(getContext(), getSID() + "-pausable-daemon-"));

        this.weakRemoteFormListener = new WeakReference<RemoteFormListener>(remoteFormListener);
        if (remoteFormListener != null) {
            remoteFormListener.formCreated(this);
        }
    }

    public RemoteFormListener getRemoteFormListener() {
        return weakRemoteFormListener.get();
    }

    public ReportGenerationData getReportData(long requestIndex, final Integer groupId, final boolean toExcel, final FormUserPreferences userPreferences) throws RemoteException {
        return processRMIRequest(requestIndex, new Callable<ReportGenerationData>() {
            @Override
            public ReportGenerationData call() throws Exception {
                return reportManager.getReportData(groupId, toExcel, userPreferences);
            }
        });
    }

    public Map<String, String> getReportPath(long requestIndex, final boolean toExcel, final Integer groupId, final FormUserPreferences userPreferences) {
        return processRMIRequest(requestIndex, new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return reportManager.getReportPath(toExcel, groupId, userPreferences);
            }
        });
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public byte[] getRichDesignByteArray() {
        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new ServerSerializationPool(new ServerContext(form.securityPolicy, richDesign)).serializeObject(new DataOutputStream(outStream), richDesign, SerializationType.GENERAL);
            //            richDesign.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public ServerResponse changePageSize(long requestIndex, final int groupID, final Integer pageSize) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                form.changePageSize(groupObject, pageSize);
            }
        });
    }

    public void gainedFocus(long requestIndex) {
        processRMIRequest(requestIndex, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                form.gainedFocus();
                return null;
            }
        });
    }

    private byte[] getFormChangesByteArray() {
        try {
            FormChanges formChanges = form.endApply();

            if (logger.isTraceEnabled()) {
                formChanges.logChanges(form, logger);
            }

            return formChanges.serialize();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public ServerResponse getRemoteChanges(long requestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                //ничего не делаем, просто даём по завершению выполниться prepareRemoteChangesResponse
            }
        });
    }

    private ImMap<ObjectInstance, Object> deserializeKeysValues(byte[] keysArray) throws IOException {
        MExclMap<ObjectInstance, Object> mMapValues = MapFact.mExclMap();
        if (keysArray != null) {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(keysArray));

            int cnt = inStream.readInt();
            for (int i = 0 ; i < cnt; ++i) {
                mMapValues.exclAdd(form.getObjectInstance(inStream.readInt()), deserializeObject(inStream));
            }
        }

        return mMapValues.immutable();
    }

    private ImMap<ObjectInstance, DataObject> deserializePropertyKeys(PropertyDrawInstance<?> propertyDraw, byte[] columnKeys) throws IOException, SQLException {
        ImMap<ObjectInstance, Object> dataKeys = deserializeKeysValues(columnKeys);

        ImFilterValueMap<ObjectInstance, DataObject> mvKeys = dataKeys.mapFilterValues();
        for (int i=0,size=dataKeys.size();i<size;i++) {
            Object value = dataKeys.getValue(i);
            //todo: для оптимизации можно забирать существующие ключи из GroupObjectInstance, чтобы сэкономить на query для чтения класса
            if (value != null) {
                ObjectInstance key = dataKeys.getKey(i);
                mvKeys.mapValue(i, form.session.getDataObject(key.getBaseClass(), value));
            }
        }
        return mvKeys.immutableValue();
    }

    private ImMap<ObjectInstance, DataObject> deserializeGroupObjectKeys(GroupObjectInstance group, byte[] treePathKeys) throws IOException {
        return group.findGroupObjectValue(deserializeKeysValues(treePathKeys));
    }

    public ServerResponse changeGroupObject(long requestIndex, final int groupID, final byte[] value) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(groupObject, value);
                if (valueToSet == null) {
                    return;
                }

                groupObject.change(form.session, valueToSet, form);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("changeGroupObject: [ID: %1$d]", groupObject.getID()));
                    logger.trace("   keys: ");
                    for (int i=0,size=valueToSet.size();i<size;i++) {
                        logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                    }
                }
            }
        });
    }

    public ServerResponse expandGroupObject(long requestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance group = form.getGroupObjectInstance(groupId);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
                if (valueToSet != null) {
                    form.expandGroupObject(group, valueToSet);
                }
            }
        });
    }

    public ServerResponse collapseGroupObject(long requestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance group = form.getGroupObjectInstance(groupId);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
                if (valueToSet != null) {
                    form.collapseGroupObject(group, valueToSet);
                }
            }
        });
    }

    public ServerResponse moveGroupObject(long requestIndex, final int parentGroupId, final byte[] parentKey, final int childGroupId, final byte[] childKey, final int index) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance parentGroup = form.getGroupObjectInstance(parentGroupId);
                GroupObjectInstance childGroup = form.getGroupObjectInstance(childGroupId);
                //todo:
//            form.moveGroupObject(parentGroup, deserializeGroupObjectKeys(parentGroup, parentKey));
            }
        });
    }

    public ServerResponse changeGroupObject(long requestIndex, final int groupID, final byte changeType) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                form.changeGroupObject(groupObject, Scroll.deserialize(changeType));
            }
        });
    }

    public ServerResponse pasteExternalTable(long requestIndex, final List<Integer> propertyIDs, final List<byte[]> columnKeys, final List<List<byte[]>> values) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();
                List<ImMap<ObjectInstance, DataObject>> keys = new ArrayList<ImMap<ObjectInstance, DataObject>>();
                for (int i =0; i < propertyIDs.size(); i++) {
                    PropertyDrawInstance<?> property = form.getPropertyDraw(propertyIDs.get(i));
                    properties.add(property);
                    keys.add(deserializePropertyKeys(property, columnKeys.get(i)));
                }
                form.pasteExternalTable(properties, keys, values);
            }
        });
    }

    public ServerResponse pasteMulticellValue(long requestIndex, final Map<Integer, List<byte[]>> bkeys, final Map<Integer, byte[]> bvalues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                Map<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> keysValues
                        = new HashMap<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>>();
                for (Map.Entry<Integer, List<byte[]>> e : bkeys.entrySet()) {
                    PropertyDrawInstance propertyDraw = form.getPropertyDraw(e.getKey());
                    Object propValue = deserializeObject(bvalues.get(e.getKey()));

                    MOrderMap<ImMap<ObjectInstance, DataObject>, Object> propKeys = MapFact.mOrderMap();
                    for (byte[] bkey : e.getValue()) {
                        propKeys.add(deserializePropertyKeys(propertyDraw, bkey), propValue);
                    }

                    keysValues.put(propertyDraw, propKeys.immutableOrder());
                }

                form.pasteMulticellValue(keysValues);
            }
        });
    }

    public ServerResponse changeGridClass(long requestIndex, final int objectID, final int idClass) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                ((CustomObjectInstance) form.getObjectInstance(objectID)).changeGridClass(idClass);
            }
        });
    }

    public ServerResponse changeClassView(long requestIndex, final int groupID, final ClassViewType classView) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.changeClassView(form.getGroupObjectInstance(groupID), classView);
            }
        });
    }

    public ServerResponse changePropertyOrder(long requestIndex, final int propertyID, final byte modiType, final byte[] columnKeys) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
                ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);
                propertyDraw.toDraw.changeOrder(propertyDraw.propertyObject.getDrawProperty().getRemappedPropertyObject(keys), Order.deserialize(modiType));
            }
        });
    }

    public ServerResponse clearPropertyOrders(long requestIndex, final int groupObjectID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.getFormInstance().getGroupObjectInstance(groupObjectID).clearOrders();
            }
        });
    }

    public int countRecords(long requestIndex, final int groupObjectID) {
        return processRMIRequest(requestIndex, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return form.countRecords(groupObjectID);
            }
        });
    }

    public Object calculateSum(long requestIndex, final int propertyID, final byte[] columnKeys) {
        return processRMIRequest(requestIndex, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
                ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);
                return form.calculateSum(propertyDraw, keys);
            }
        });
    }

    public Map<List<Object>, List<Object>> groupData(long requestIndex, final Map<Integer, List<byte[]>> groupMap, final Map<Integer, List<byte[]>> sumMap,
                                                     final Map<Integer, List<byte[]>> maxMap, final boolean onlyNotNull) {
        return processRMIRequest(requestIndex, new Callable<Map<List<Object>, List<Object>>>() {
            @Override
            public Map<List<Object>, List<Object>> call() throws Exception {
                List<Map<Integer, List<byte[]>>> inMaps = new ArrayList<Map<Integer, List<byte[]>>>(BaseUtils.toList(groupMap, sumMap, maxMap));
                List<ImMap<Object, ImList<ImMap<ObjectInstance, DataObject>>>> outMaps = new ArrayList<ImMap<Object, ImList<ImMap<ObjectInstance, DataObject>>>>();
                for (Map<Integer, List<byte[]>> one : inMaps) {
                    MExclMap<Object, ImList<ImMap<ObjectInstance, DataObject>>> mOutMap = MapFact.mExclMap(one.size());
                    for (Map.Entry<Integer, List<byte[]>> oneEntry : one.entrySet()) {
                        PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(oneEntry.getKey());
                        MList<ImMap<ObjectInstance, DataObject>> mList = ListFact.mList();
                        if (propertyDraw != null) {
                            for (byte[] columnKeys : oneEntry.getValue()) {
                                mList.add(deserializePropertyKeys(propertyDraw, columnKeys));
                            }
                            mOutMap.exclAdd(propertyDraw, mList.immutableList());
                        } else
                            mOutMap.exclAdd(0, ListFact.<ImMap<ObjectInstance, DataObject>>EMPTY());
                    }
                    outMaps.add(mOutMap.immutable());
                }
                return form.groupData(BaseUtils.<ImMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>>>immutableCast(outMaps.get(0)),
                                      outMaps.get(1), BaseUtils.<ImMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>>>immutableCast(outMaps.get(2)), onlyNotNull);
            }
        });
    }

    public ServerResponse setUserFilters(long requestIndex, final byte[][] filters) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                for (GroupObjectInstance group : form.getGroups()) {
                    group.clearUserFilters();
                }
                for (byte[] state : filters) {
                    FilterInstance filter = FilterInstance.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
                    filter.getApplyObject().addUserFilter(filter);
                }
            }
        });
    }

    public ServerResponse setRegularFilter(long requestIndex, final int groupID, final int filterID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.setRegularFilter(form.getRegularFilterGroup(groupID), filterID);
            }
        });
    }

    public int getID() {
        return form.entity.getID();
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public String getSID() {
        return form.entity.getSID();
    }

    public ServerResponse closedPressed(long requestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.onQueryClose();
            }
        });
    }

    public ServerResponse okPressed(long requestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.onQueryOk();
            }
        });
    }

    public ServerResponse setTabVisible(long requestIndex, final int tabPaneID, final int tabIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.setTabVisible((ContainerView) richDesign.findById(tabPaneID), richDesign.findById(tabIndex));
            }
        });
    }

    @Override
    public void saveUserPreferences(long requestIndex, final FormUserPreferences preferences, final boolean forAllUsers) throws RemoteException {
        processRMIRequest(requestIndex, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                form.saveUserPreferences(preferences, forAllUsers);
                return null;
            }
        });
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public FormUserPreferences getUserPreferences() throws RemoteException {
        return form.loadUserPreferences();
    }

    /**
     * готовит форму для восстановленного подключения
     */
    public void invalidate() throws SQLException {
        form.refreshData();
        if (currentInvocation != null && currentInvocation.isPaused()) {
            try {
                currentInvocation.cancel();
            } catch (Exception e) {
                logger.warn("Exception was thrown, while invalidating form", e);
            }
        }
    }

    public ServerResponse changeProperty(long requestIndex, final int propertyID, final byte[] fullKey, final byte[] pushChange, final Integer pushAdd) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, fullKey);

                ObjectValue pushChangeObject = null;
                if (pushChange != null) {
                    pushChangeObject = DataObject.getValue(deserializeObject(pushChange), propertyDraw.getEntity().getRequestInputType(form.entity));
                }

                DataObject pushAddObject = null;
                if (pushAdd != null) {
                    pushAddObject = new DataObject(pushAdd, form.session.baseClass.unknown);
                }

                form.executeEditAction(propertyDraw, ServerResponse.CHANGE, keys, pushChangeObject, pushAddObject, true);
            }
        });
    }

    public ServerResponse executeEditAction(long requestIndex, final int propertyID, final byte[] columnKey, final String actionSID) throws RemoteException {
        if (!isEditing.compareAndSet(false, true)) {
            return ServerResponse.EMPTY;
        }
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                try {
                    PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                    ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);

                    form.executeEditAction(propertyDraw, actionSID, keys);

                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("executeEditAction: [ID: %1$d, SID: %2$s]", propertyDraw.getID(), propertyDraw.getsID()));
                        if (keys.size() > 0) {
                            logger.trace("   columnKeys: ");
                            for (int i=0,size=keys.size();i<size;i++) {
                                logger.trace(String.format("     %1$s == %2$s", keys.getKey(i), keys.getValue(i)));
                            }
                        }

                        if (logger.isTraceEnabled()) {
                            logger.trace("   current object's values: ");
                            for (ObjectInstance obj : form.getObjects()) {
                                logger.trace(String.format("     %1$s == %2$s", obj, obj.getObjectValue()));
                            }
                        }
                    }
                } finally {
                    isEditing.set(false);
                }
            }
        });
    }

    private <T> T processRMIRequest(long requestIndex, Callable<T> request) {
        String invocationSID = generateInvocationSid(requestIndex);

        requestLock.acquireRequestLock(invocationSID, requestIndex);
        try {
            return request.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            requestLock.releaseRequestLock(invocationSID, requestIndex);
        }
    }

    private ServerResponse executeServerInvocation(long requestIndex, RemotePausableInvocation invocation) throws RemoteException {
        numberOfFormChangesRequests.incrementAndGet();
        requestLock.acquireRequestLock(invocation.getSID(), requestIndex);

        currentInvocation = invocation;
        return invocation.execute();
    }

    private ServerResponse processPausableRMIRequest(final long requestIndex, final ERunnable runnable) throws RemoteException {

        return executeServerInvocation(requestIndex, new RemotePausableInvocation(generateInvocationSid(requestIndex), pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                runnable.run();
                return prepareRemoteChangesResponse(delayedActions, delayedGetRemoteChanges, delayedHideForm);
            }

            @Override
            protected ServerResponse handleFinished() throws RemoteException {
                unlockNextRmiRequest();
                return super.handleFinished();
            }

            @Override
            protected ServerResponse handleThrows(Throwable t) throws RemoteException {
                unlockNextRmiRequest();
                return super.handleThrows(t);
            }

            private void unlockNextRmiRequest() {
                currentInvocation = null;
                int left = numberOfFormChangesRequests.decrementAndGet();
                assert left >= 0;
                requestLock.releaseRequestLock(getSID(), requestIndex);
            }
        });
    }

    private ServerResponse prepareRemoteChangesResponse(List<ClientAction> pendingActions, boolean delayedGetRemoteChanges, boolean delayedHideForm) {
        if (numberOfFormChangesRequests.get() > 1 || delayedGetRemoteChanges) {
            return new ServerResponse(pendingActions.toArray(new ClientAction[pendingActions.size()]), false);
        }

        byte[] formChanges = getFormChangesByteArray();

        List<ClientAction> resultActions = new ArrayList<ClientAction>();
        resultActions.add(new ProcessFormChangesClientAction(formChanges));

        resultActions.addAll(pendingActions);

        if (delayedHideForm) {
            unexportLater();
        }

        return new ServerResponse(resultActions.toArray(new ClientAction[resultActions.size()]), false);
    }

    private String generateInvocationSid(long requestIndex) {
        String invocationSID;
        if (ServerLoggers.pausablesInvocationLogger.isDebugEnabled()) {
            StackTraceElement[] st = new Throwable().getStackTrace();
            String methodName = st[2].getMethodName();

            int aspectPostfixInd = methodName.indexOf("_aroundBody");
            if (aspectPostfixInd != -1) {
                methodName = methodName.substring(0, aspectPostfixInd);
            }

            invocationSID = "[f: " + getSID() + ", m: " + methodName + ", rq: " + requestIndex + "]";
        } else {
            invocationSID = "";
        }
        return invocationSID;
    }

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    public ServerResponse throwInServerInvocation(Throwable clientThrowable) throws RemoteException {
        return currentInvocation.resumeWithThrowable(clientThrowable);
    }

    String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    Object[] requestUserInteraction(ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    @Override
    public void unexportNow() {
        RemoteFormListener listener = getRemoteFormListener();
        if (listener != null) {
            listener.formDestroyed(this);
        }
        super.unexportNow();
    }
}
