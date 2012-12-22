package platform.server.form.instance.remote;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.ERunnable;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ProcessFormChangesClientAction;
import platform.interop.action.UpdateCurrentClassClientAction;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportGenerationData;
import platform.interop.form.ServerResponse;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.RemoteContextObject;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.form.view.ContainerView;
import platform.server.form.view.FormView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static platform.base.BaseUtils.deserializeObject;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>, F extends FormInstance<T>> extends RemoteContextObject implements RemoteFormInterface {
    private final static Logger logger = Logger.getLogger(RemoteForm.class);

    public final F form;
    private final FormView richDesign;
    public final FormReportManager<T, F> reportManager;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    private final ExecutorService pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(this, "-pausable-daemon-"));
    private final AtomicInteger numberOfFormChangesRequests = new AtomicInteger();
    private final AtomicBoolean isEditing = new AtomicBoolean(false);
    private final SequentialRequestLock requestLock;
    private RemotePausableInvocation currentInvocation = null;

    public RemoteForm(F form, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        super(port);

        this.form = form;
        this.richDesign = form.entity.getRichDesign();
        this.reportManager = new FormReportManager(form);
        this.requestLock = new SequentialRequestLock(getSID());

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
            new ServerSerializationPool(new ServerContext(richDesign)).serializeObject(new DataOutputStream(outStream), richDesign, SerializationType.GENERAL);
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

    private ServerResponse prepareRemoteChangesResponse(List<ClientAction> pendingActions, boolean delayRemoteChanges) {
        if (numberOfFormChangesRequests.get() > 1 || delayRemoteChanges) {
            //todo: возможно стоит сохранять количество пропущенных изменений, и высылать таки их, если пропустили слишком много
            return new ServerResponse(pendingActions.toArray(new ClientAction[pendingActions.size()]), false, delayRemoteChanges);
        }

        byte[] formChanges = getFormChangesByteArray();

        List<ClientAction> resultActions = new ArrayList<ClientAction>();
        resultActions.add(new ProcessFormChangesClientAction(formChanges));

        if (updateCurrentClass != null) {
            ConcreteCustomClass currentClass = form.getObjectClass(updateCurrentClass);
            RemoteFormListener remoteFormListener = getRemoteFormListener();
            if (currentClass != null && remoteFormListener != null && remoteFormListener.currentClassChanged(currentClass)) {
                resultActions.add(new UpdateCurrentClassClientAction(currentClass.ID));
            }

            updateCurrentClass = null;
        }

        resultActions.addAll(pendingActions);
        return new ServerResponse(resultActions.toArray(new ClientAction[resultActions.size()]), false);
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
            if (value != null)
                mvKeys.mapValue(i, form.session.getDataObject(value, dataKeys.getKey(i).getType()));
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

                groupObject.change(form.session, valueToSet);

                updateCurrentClass = groupObject.objects.iterator().next();

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
                updateCurrentClass = groupObject.objects.iterator().next();
            }
        });
    }

    private ObjectInstance updateCurrentClass = null;

    public ServerResponse pasteExternalTable(long requestIndex, final List<Integer> propertyIDs, final List<List<Object>> table) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.pasteExternalTable(propertyIDs, table);
            }
        });
    }

    public ServerResponse pasteMulticellValue(long requestIndex, final Map<Integer, List<Map<Integer, Object>>> cells, final Object value) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.pasteMulticellValue(cells, value);
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
                propertyDraw.toDraw.changeOrder(((CalcPropertyObjectInstance<?>) propertyDraw.propertyObject).getRemappedPropertyObject(keys), Order.deserialize(modiType));
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
                form.formClose();
            }
        });
    }

    public ServerResponse okPressed(long requestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, new ERunnable() {
            @Override
            public void run() throws Exception {
                form.formOk();
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
    public void saveUserPreferences(long requestIndex, final FormUserPreferences preferences, final Boolean forAllUsers) throws RemoteException {
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

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        try {
            return new RemoteDialog(dialogInstance, exportPort, getRemoteFormListener());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean interactive) throws SQLException {
        return form.createForm(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, interactive);
    }

    @Override
    public BusinessLogics getBL() {
        return form.BL;
    }

    public FormInstance getFormInstance() {
        return form;
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
                    pushChangeObject = DataObject.getValue(deserializeObject(pushChange), (ConcreteClass) propertyDraw.getEntity().getChangeType(form.entity));
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
            return ServerResponse.empty;
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
        requestLock.acquireRequestLock(requestIndex);
        try {
            return request.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            requestLock.releaseRequestLock(requestIndex);
        }
    }

    private ServerResponse executeServerInvocation(long requestIndex, RemotePausableInvocation invocation) throws RemoteException {
        numberOfFormChangesRequests.incrementAndGet();
        requestLock.acquireRequestLock(requestIndex);

        currentInvocation = invocation;
        return invocation.execute();
    }

    private ServerResponse processPausableRMIRequest(final long requestIndex, final ERunnable runnable) throws RemoteException {
        return executeServerInvocation(requestIndex, new RemotePausableInvocation("f#" + getSID() + "_rq#" + requestIndex, pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                runnable.run();
                return prepareRemoteChangesResponse(delayedActions, delayRemoteChanges);
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
                requestLock.releaseRequestLock(requestIndex);
            }
        });
    }

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    public ServerResponse throwInServerInvocation(Exception clientException) throws RemoteException {
        return currentInvocation.resumWithException(clientException);
    }

    public String getLogMessage() {
        return currentInvocation.getLogMessage();
    }

    public void delayRemoteChanges() {
        currentInvocation.delayRemoteChanges();
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }
}
