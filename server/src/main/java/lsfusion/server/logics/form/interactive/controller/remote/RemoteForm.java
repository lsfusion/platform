package lsfusion.server.logics.form.interactive.controller.remote;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ProcessFormChangesClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.interop.form.stat.report.ReportGenerationData;
import lsfusion.interop.form.user.*;
import lsfusion.server.base.controller.remote.SequentialRequestLock;
import lsfusion.server.base.controller.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.controller.remote.ui.RemotePausableInvocation;
import lsfusion.server.base.controller.stack.ThrowableWithStack;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.EExecutionStackCallable;
import lsfusion.server.logics.action.controller.stack.EExecutionStackRunnable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.controller.context.RemoteFormContext;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.InteractiveFormReportManager;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Optional.fromNullable;
import static lsfusion.base.BaseUtils.deserializeObject;

// фасад для работы с клиентом
public class RemoteForm<F extends FormInstance> extends ContextAwarePendingRemoteObject implements RemoteFormInterface {
    private final static Logger logger = ServerLoggers.remoteLogger;

    public final F form;
    private final FormView richDesign;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    private final AtomicInteger numberOfFormChangesRequests = new AtomicInteger();
    private final SequentialRequestLock requestLock;
    private RemotePausableInvocation currentInvocation = null;

    private final Map<Long, Optional<?>> recentResults = Collections.synchronizedMap(new HashMap<Long, Optional<?>>());
    private final Map<Long, Integer> requestsContinueIndices = Collections.synchronizedMap(new HashMap<Long, Integer>());

    private long minReceivedRequestIndex = 0;

    private Map<Long, SyncExecution> syncExecuteServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncContinueServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncThrowInServerInvocationMap = MapFact.mAddRemoveMap();
    private Map<Long, SyncExecution> syncProcessRMIRequestMap = MapFact.mAddRemoveMap();

    public RemoteForm(F form, int port, RemoteFormListener remoteFormListener, ExecutionStack upStack) throws RemoteException {
        super(port, upStack, form.entity.getSID(), form.isSync() ? SyncType.SYNC : SyncType.NOSYNC);

        setContext(new RemoteFormContext<>(this));
        this.form = form;
        this.richDesign = form.entity.getRichDesign();
        this.requestLock = new SequentialRequestLock();

        this.weakRemoteFormListener = new WeakReference<>(remoteFormListener);
        createPausablesExecutor();

        remoteFormListener.formCreated(this);
    }

    public RemoteFormListener getRemoteFormListener() {
        return weakRemoteFormListener.get();
    }

    public ReportGenerationData getReportData(long requestIndex, long lastReceivedRequestIndex, final Integer groupId, final FormPrintType printType, final FormUserPreferences userPreferences) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<ReportGenerationData>() {
            @Override
            public ReportGenerationData call(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("getReportData Action. GroupID: %s", groupId));
                }

                return new InteractiveFormReportManager(form, groupId, userPreferences).getReportData(printType);
            }
        });
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается,
     * что он отработаывает как ImmutableMethod через createAndExecute
     */
    public byte[] getRichDesignByteArray() {
        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new ServerSerializationPool(new ServerContext(form.securityPolicy, richDesign, form.BL)).serializeObject(new DataOutputStream(outStream), richDesign);
            //            richDesign.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается,
     * что он отработаывает как ImmutableMethod через createAndExecute
     */
    public Integer getInitFilterPropertyDraw() throws RemoteException {
        return null; // deprecated
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public FormUserPreferences getUserPreferences() throws RemoteException {

        FormUserPreferences result = form.loadUserPreferences();
        
        if (logger.isTraceEnabled()) {
            logger.trace("getUserPreferences Action");
        }
        
        return result;
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public ColorPreferences getColorPreferences() throws RemoteException {

        ColorPreferences result = form.loadColorPreferences();

        if (logger.isTraceEnabled()) {
            logger.trace("getColorPreferences Action");
        }

        return result;
    }

    public ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, final int groupID, final Integer pageSize) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                if (logger.isTraceEnabled()) {
                    GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                    logger.trace(String.format("changePageSize: [ID: %1$d]", groupObject.getID()));
                    logger.trace(String.format("new page size %s:", pageSize));
                }
                
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                form.changePageSize(groupObject, pageSize);
            }
        });
    }

    public void gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<Void>() {
            @Override
            public Void call(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace("gainedFocus Action");                    
                }
                
                form.gainedFocus(stack);
                return null;
            }
        });
    }

    public ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, final boolean refresh) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                if (refresh) {
                    form.refreshData();
                }
            }
        });
    }

    private static DataInputStream getDeserializeKeysValuesInputStream(byte[] keysArray) {
        assert keysArray != null;
        return keysArray != null ? new DataInputStream(new ByteArrayInputStream(keysArray)) : null;
    }
    private ImMap<ObjectInstance, Object> deserializeKeysValues(byte[] keysArray) throws IOException {
        return deserializeKeysValues(getDeserializeKeysValuesInputStream(keysArray), form);
    }
    private static ImMap<ObjectInstance, Object> deserializeKeysValues(DataInputStream inStream, FormInstance form) throws IOException {
        MExclMap<ObjectInstance, Object> mMapValues = MapFact.mExclMap();
        if (inStream != null) {
            int cnt = inStream.readInt();
            for (int i = 0 ; i < cnt; ++i) {
                mMapValues.exclAdd(form.getObjectInstance(inStream.readInt()), deserializeObject(inStream));
            }
        } else 
            assert false;

        return mMapValues.immutable();
    }

    private ImMap<ObjectInstance, DataObject> deserializePropertyKeys(PropertyDrawInstance<?> propertyDraw, byte[] remapKeys) throws IOException, SQLException, SQLHandledException {
        return deserializePropertyKeys(propertyDraw, getDeserializeKeysValuesInputStream(remapKeys), form);
    }
    public static ImMap<ObjectInstance, DataObject> deserializePropertyKeys(PropertyDrawInstance<?> propertyDraw, DataInputStream remapKeys, FormInstance form) throws IOException, SQLException, SQLHandledException {
        ImMap<ObjectInstance, Object> dataKeys = deserializeKeysValues(remapKeys, form);

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

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupID, final byte[] value) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(groupObject, value);
                if (valueToSet == null) {
                    return;
                }

                groupObject.change(form.session, valueToSet, form, stack);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("changeGroupObject: [ID: %1$d]", groupObject.getID()));
                    logger.trace("   keys: ");
                    for (int i = 0, size = valueToSet.size(); i < size; i++) {
                        logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                    }
                }
            }
        });
    }

    public ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                GroupObjectInstance group = form.getGroupObjectInstance(groupId);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
                if (valueToSet != null) {
                    if (logger.isTraceEnabled()) {
                        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupId);
                        logger.trace(String.format("expandGroupObject: [ID: %1$d]", groupObject.getID()));
                        logger.trace("   keys: ");
                        for (int i = 0, size = valueToSet.size(); i < size; i++) {
                            logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                        }
                    }
                    form.expandGroupObject(group, valueToSet);
                }
            }
        });
    }

    public ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                GroupObjectInstance group = form.getGroupObjectInstance(groupId);
                ImMap<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
                if (valueToSet != null) {
                    if (logger.isTraceEnabled()) {
                        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupId);
                        logger.trace(String.format("collapseGroupObject: [ID: %1$d]", groupObject.getID()));
                        logger.trace("   keys: ");
                        for (int i = 0, size = valueToSet.size(); i < size; i++) {
                            logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                        }
                    }
                    form.collapseGroupObject(group, valueToSet);
                }
            }
        });
    }

    public ServerResponse moveGroupObject(long requestIndex, long lastReceivedRequestIndex, final int parentGroupId, final byte[] parentKey, final int childGroupId, final byte[] childKey, final int index) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                GroupObjectInstance parentGroup = form.getGroupObjectInstance(parentGroupId);
                GroupObjectInstance childGroup = form.getGroupObjectInstance(childGroupId);
                //todo:
//            form.moveGroupObject(parentGroup, deserializeGroupObjectKeys(parentGroup, parentKey));
            }
        });
    }

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupID, final byte changeType) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("changePageSize: [ID: %1$d]", groupObject.getID()));
                    logger.trace(String.format("new type: %s", changeType));
                }
                
                form.changeGroupObject(groupObject, Scroll.deserialize(changeType));
            }
        });
    }

    public ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, final List<Integer> propertyIDs, final List<byte[]> columnKeys, final List<List<byte[]>> values) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                List<PropertyDrawInstance> properties = new ArrayList<>();
                List<ImMap<ObjectInstance, DataObject>> keys = new ArrayList<>();
                for (int i =0; i < propertyIDs.size(); i++) {
                    PropertyDrawInstance<?> property = form.getPropertyDraw(propertyIDs.get(i));
                    properties.add(property);
                    keys.add(deserializePropertyKeys(property, columnKeys.get(i)));
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("pasteExternalTable Action");

                    for (int i =0; i < propertyIDs.size(); i++) {
                        logger.trace(String.format("%s-%s", form.getPropertyDraw(propertyIDs.get(i)).getSID(), String.valueOf(columnKeys.get(i))));
                    }                  
                }
                
                form.pasteExternalTable(properties, keys, values, stack);
            }
        });
    }

    public ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, final Map<Integer, List<byte[]>> bkeys, final Map<Integer, byte[]> bvalues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                Map<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> keysValues
                        = new HashMap<>();

                if (logger.isTraceEnabled())
                    logger.trace("pasteMultiCellValue Action");
                
                for (Map.Entry<Integer, List<byte[]>> e : bkeys.entrySet()) {
                    PropertyDrawInstance propertyDraw = form.getPropertyDraw(e.getKey());
                    Object propValue = deserializeObject(bvalues.get(e.getKey()));

                    MOrderMap<ImMap<ObjectInstance, DataObject>, Object> propKeys = MapFact.mOrderMap();
                    for (byte[] bkey : e.getValue()) {
                        
                        if(logger.isTraceEnabled())
                            logger.trace(String.format("propertyDraw: %s", propertyDraw.getSID()));
                        
                        propKeys.add(deserializePropertyKeys(propertyDraw, bkey), propValue);
                    }

                    keysValues.put(propertyDraw, propKeys.immutableOrder());
                }

                form.pasteMulticellValue(keysValues, stack);
            }
        });
    }

    public ServerResponse changeGridClass(long requestIndex, long lastReceivedRequestIndex, final int objectID, final long idClass) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("changeGridClass: [ID: %1$d]", objectID));
                    logger.trace(String.format("new grid class id: %s", idClass));
                }
                
                ((CustomObjectInstance) form.getObjectInstance(objectID)).changeGridClass(idClass);
            }
        });
    }

    public ServerResponse changeClassView(long requestIndex, long lastReceivedRequestIndex, final int groupID, final ClassViewType classView) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("changeClassView: [ID: %1$d]", groupObject.getID()));
                    logger.trace(String.format("new classView: %s", String.valueOf(classView)));
                }
                
                form.changeClassView(form.getGroupObjectInstance(groupID), classView);
            }
        });
    }

    public ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte modiType, final byte[] columnKeys) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
                if(propertyDraw != null) {
                    ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);

                    Order order = Order.deserialize(modiType);

                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("changePropertyOrder: [ID: %1$d]", propertyID));
                        logger.trace(String.format("new order: %s", order.toString()));
                    }

                    propertyDraw.toDraw.changeOrder(propertyDraw.getDrawInstance().getRemappedPropertyObject(keys), Order.deserialize(modiType));
                }
            }
        });
    }

    public ServerResponse clearPropertyOrders(long requestIndex, long lastReceivedRequestIndex, final int groupObjectID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupObjectID);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("clearPropertyOrders: [ID: %1$d]", groupObject.getID()));
                }
                
                form.getGroupObjectInstance(groupObjectID).clearOrders();
            }
        });
    }

    public int countRecords(long requestIndex, long lastReceivedRequestIndex, final int groupObjectID) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<Integer>() {
            @Override
            public Integer call(ExecutionStack stack) throws Exception {

                int result = form.countRecords(groupObjectID);

                if (logger.isTraceEnabled()) {
                    GroupObjectInstance groupObject = form.getGroupObjectInstance(groupObjectID);
                    logger.trace(String.format("countRecords Action. GroupObjectID: %s. Result: %s", groupObject.getID(), result));
                }

                return result;
            }
        });
    }

    public Object calculateSum(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte[] columnKeys) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<Object>() {
            @Override
            public Object call(ExecutionStack stack) throws Exception {
                PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
                ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);

                Object result = form.calculateSum(propertyDraw, keys);
                
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("calculateSum Action. propertyDrawID: %s. Result: %s", propertyDraw.getSID(), result));
                }
                
                return result;
            }
        });
    }

    public byte[] groupData(long requestIndex, long lastReceivedRequestIndex, final Map<Integer, List<byte[]>> groupMap, final Map<Integer, List<byte[]>> sumMap,
                                                     final Map<Integer, List<byte[]>> maxMap, final boolean onlyNotNull) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<byte[]>() {
            @Override
            public byte[] call(ExecutionStack stack) throws Exception {
                List<Map<Integer, List<byte[]>>> inMaps = Arrays.asList(groupMap, sumMap, maxMap);
                List<ImOrderMap<Object, ImList<ImMap<ObjectInstance, DataObject>>>> outMaps = new ArrayList<>();
                for (Map<Integer, List<byte[]>> one : inMaps) {
                    MOrderExclMap<Object, ImList<ImMap<ObjectInstance, DataObject>>> mOutMap = MapFact.mOrderExclMap(one.size());
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
                    outMaps.add(mOutMap.immutableOrderCopy());
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("groupData Action");
                }

                Map<List<Object>, List<Object>> grouped = form.groupData(BaseUtils.<ImOrderMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>>>immutableCast(outMaps.get(0)),
                        outMaps.get(1), BaseUtils.<ImOrderMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>>>immutableCast(outMaps.get(2)), onlyNotNull);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream outStream = new DataOutputStream(out);
                outStream.writeInt(grouped.size());
                for (Map.Entry<List<Object>, List<Object>> entry : grouped.entrySet()) {
                    outStream.writeInt(entry.getKey().size());
                    for (Object key : entry.getKey()) {
                        BaseUtils.serializeObject(outStream, key);
                    }
                    
                    outStream.writeInt(entry.getValue().size());
                    for (Object value : entry.getValue()) {
                        BaseUtils.serializeObject(outStream, value);
                    }
                }
                
                return out.toByteArray();
            }
        });
    }

    @Override
    public List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, final String groupObjectSID) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<List<FormGrouping>>() {
            @Override
            public List<FormGrouping> call(ExecutionStack stack) throws Exception {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("readGroupings Action. GroupObjectSID: %s", groupObjectSID));
                }
                return form.readGroupings(groupObjectSID);
            }
        });
    }

    @Override
    public void saveGrouping(long requestIndex, long lastReceivedRequestIndex, final FormGrouping grouping) throws RemoteException {
        processRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackCallable<Void>() {
            @Override
            public Void call(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("readGroupings Action: [ID: %s]", grouping.groupObjectSID));
                }
                
                form.saveGrouping(grouping, stack);
                return null;
            }
        });
    }

    public ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, final byte[][] filters) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                for (GroupObjectInstance group : form.getGroups()) {
                    group.clearUserFilters();
                }
                for (byte[] state : filters) {
                    FilterInstance filter = FilterInstance.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
                    GroupObjectInstance applyObject = filter.getApplyObject();
                    if(applyObject != null) {
                        applyObject.addUserFilter(filter);
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("set user filter: [CLASS: %1$s]", filter.getClass()));
                            logger.trace(String.format("apply object: %s", filter.getApplyObject().getID()));
                        }
                    }
                }
            }
        });
    }

    public ServerResponse setRegularFilter(long requestIndex, long lastReceivedRequestIndex, final int groupID, final int filterID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                form.setRegularFilter(form.getRegularFilterGroup(groupID), filterID);
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("set regular filter: [GROUP: %1$s]", groupID));
                    logger.trace(String.format("filter ID: %s", filterID));
                }
            }
        });
    }

    public String getCanonicalName() {
        return form.entity.getCanonicalName();
    }

    public ServerResponse closedPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace("closedPressed Action");
                }

                form.formQueryClose(stack);
            }
        });
    }

    public ServerResponse okPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                
                if (logger.isTraceEnabled()) {
                    logger.trace("okPressed Action");
                }
                
                form.formQueryOk(stack);
            }
        });
    }

    public ServerResponse setTabVisible(long requestIndex, long lastReceivedRequestIndex, final int tabPaneID, final int childId) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace("setTabVisible Action");
                }
                
                form.setTabVisible((ContainerView) richDesign.findById(tabPaneID), richDesign.findById(childId));
            }
        });
    }

    @Override
    public ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, final GroupObjectUserPreferences preferences, final boolean forAllUsers, final boolean completeOverride, final String[] hiddenProps) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {

                if (logger.isTraceEnabled()) {
                    logger.trace("saveUserPreferences Action");
                }
                
                form.saveUserPreferences(stack, preferences, forAllUsers, completeOverride);
                
                form.refreshUPHiddenProperties(preferences.groupObjectSID, hiddenProps);
            }
        });
    }

    @Override
    public ServerResponse refreshUPHiddenProperties(long requestIndex, long lastReceivedRequestIndex, final String groupObjectSID, final String[] propSids) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                form.refreshUPHiddenProperties(groupObjectSID, propSids);
            }
        });
    }

    public ServerResponse changeProperty(final long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte[] fullKey, final byte[] pushChange, final Long pushAdd) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                changeProperty(stack, propertyDraw, fullKey, pushChange, pushAdd);
            }
        });
    }

    @Override
    public ServerResponse changePropertyExternal(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final String value) throws IOException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                changeProperty(stack, propertyDraw, null, BaseUtils.serializeObject(propertyDraw.getType().parseString(value)), null);
            }
        });
    }

    private void changeProperty(ExecutionStack stack, final PropertyDrawInstance propertyDraw, final byte[] fullKey, final byte[] pushChange, final Long pushAdd) throws SQLHandledException, SQLException, IOException {
        ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, fullKey);

        ObjectValue pushChangeObject = null;
        DataClass pushChangeType = null;
        if (pushChange != null) {
            pushChangeType = propertyDraw.getEntity().getRequestInputType(form.securityPolicy);
            Object objectPushChange = deserializeObject(pushChange);
            if(pushChangeType == null) // веб почему-то при асинхронном удалении шлет не null, а [0] который deserialize'ся в null а потом превращается в NullValue.instance и падают ошибки
                ServerLoggers.assertLog(objectPushChange == null, "PUSH CHANGE SHOULD BE NULL");
            else
                pushChangeObject = DataObject.getValue(objectPushChange, pushChangeType);
        }

        DataObject pushAddObject = null;
        if (pushAdd != null) {
            pushAddObject = new DataObject(pushAdd, form.session.baseClass.unknown);
        }

        form.executeEditAction(propertyDraw, ServerResponse.CHANGE, keys, pushChangeObject, pushChangeType, pushAddObject, true, stack);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("changeProperty: [ID: %1$d, SID: %2$s]", propertyDraw.getID(), propertyDraw.getSID()));
            if (keys.size() > 0) {
                logger.trace("   columnKeys: ");
                for (int i = 0, size = keys.size(); i < size; i++) {
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
    }

    @Override
    public void closeExternal() {
        try {
            form.explicitClose();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }
    }

    public ServerResponse executeEditAction(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte[] fullKey, final String actionSID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            @Override
            public void run(ExecutionStack stack) throws Exception {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                ImMap<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, fullKey);

                form.executeEditAction(propertyDraw, actionSID, keys, stack);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("executeEditAction: [ID: %1$d, SID: %2$s]", propertyDraw.getID(), propertyDraw.getSID()));
                    if (keys.size() > 0) {
                        logger.trace("   columnKeys: ");
                        for (int i = 0, size = keys.size(); i < size; i++) {
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
            }
        });
    }

    public ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, final int idNotification) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, new EExecutionStackRunnable() {
            public void run(ExecutionStack stack) throws Exception {
                RemoteFormListener remoteNavigator = getRemoteFormListener();
                if(remoteNavigator != null) {
                    remoteNavigator.executeNotificationAction(form, stack, idNotification);
                }
            }
        });
    }

    private <T> T processRMIRequest(long requestIndex, long lastReceivedRequestIndex, final EExecutionStackCallable<T> request) throws RemoteException {
        Optional<?> optionalResult = recentResults.get(requestIndex);
        if (optionalResult != null) {
            assert requestIndex >= minReceivedRequestIndex;
            return optionalResult(optionalResult);
        }
        
        if(requestIndex != -1 && requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded
            return null; // this check is important, because otherwise acquireRequestLock will never stop

        String invocationSID = generateInvocationSid(requestIndex);

        requestLock.acquireRequestLock(invocationSID, requestIndex);
        try {
            return callAndCacheResult(requestIndex, lastReceivedRequestIndex, new Callable<T>() {
                public T call() throws Exception {
                    return request.call(getStack());
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            requestLock.releaseRequestLock(invocationSID, requestIndex);
        }
    }

    private ServerResponse executeServerInvocation(long requestIndex, long lastReceivedRequestIndex, RemotePausableInvocation invocation) throws RemoteException {
        Optional<?> optionalResult = recentResults.get(requestIndex);
        if (optionalResult != null) {
            ServerLoggers.pausableLog("Return cachedResult for: " + requestIndex);
            return optionalResult(optionalResult);
        }

        if(requestIndex != -1 && requestIndex < minReceivedRequestIndex) // request can be lost and reach server only after retried and even next request already received, proceeded  
            return null; // this check is important, because otherwise acquireRequestLock will never stop and numberOfFormChangesRequests will be always > 0

        numberOfFormChangesRequests.incrementAndGet();
        requestLock.acquireRequestLock(invocation.getSID(), requestIndex);

        currentInvocation = invocation;

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, invocation);
    }

    private <T> T callAndCacheResult(long requestIndex, long lastReceivedRequestIndex, Callable<T> request) throws RemoteException {
        clearRecentResults(lastReceivedRequestIndex);

        Object result;
        try {
            result = request.call();
        } catch (Throwable t) {
            result = new ThrowableWithStack(t);
        }

        if (requestIndex != -1) {
            recentResults.put(requestIndex, fromNullable(result));
        }

        return cachedResult(result);
    }

    private <T> T optionalResult(Optional<?> optionalResult) throws RemoteException {
        if (!optionalResult.isPresent()) {
            return null;
        }

        return cachedResult(optionalResult.get());
    }

    /**
     * Если result instanceof Throwable, выбрасывает Exception, иначе кастит к T
     */
    private <T> T cachedResult(Object result) throws RemoteException {
        if (result instanceof ThrowableWithStack) {
            throw ((ThrowableWithStack) result).propagateRemote();
        } else {
            return (T) result;
        }
    }

    private void clearRecentResults(long lastReceivedRequestIndex) {
        //assert: current thread holds the request lock
        if (lastReceivedRequestIndex == -2) {
            recentResults.clear();
        } else {
            if(lastReceivedRequestIndex == -1)
                return;
            // if request is already received, there is no need for recentResults for that request (we'll assume that there cannot be retryableRequest after that)
            // however it is < (and not <=) because the same requestIndex is used for continueServerInvocation (so it would be possible to clear result that might be needed for retryable request)
            // the cleaner solution is to lookahead if there will be continueServerInvocation and don't set lastReceivedRequestIndex in RmiQueue in that case, but now using < is a lot easier
            for (long i = minReceivedRequestIndex; i < lastReceivedRequestIndex; ++i) {
                recentResults.remove(i);
            }
            minReceivedRequestIndex = lastReceivedRequestIndex;
        }
    }

    private ServerResponse processPausableRMIRequest(final long requestIndex, long lastReceivedRequestIndex, final EExecutionStackRunnable runnable) throws RemoteException {

        return executeServerInvocation(requestIndex, lastReceivedRequestIndex, new RemotePausableInvocation(requestIndex, generateInvocationSid(requestIndex), pausablesExecutor, this) {
            @Override
            protected ServerResponse callInvocation() throws Throwable {
                ExecutionStack stack = getStack();
                runnable.run(stack);
                return prepareRemoteChangesResponse(requestIndex, delayedActions, delayedGetRemoteChanges, delayedHideForm, stack);
            }

            @Override
            protected ServerResponse handleFinished() throws RemoteException {
                unlockNextRmiRequest();
                return super.handleFinished();
            }

            @Override
            protected ServerResponse handleThrows(ThrowableWithStack t) throws RemoteException {
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

    // именно непосредственно перед возвращением результата, иначе closeLater может сработать сильно раньше
    private ServerResponse returnRemoteChangesResponse(long requestIndex, List<ClientAction> pendingActions, boolean delayedHideForm, ExecutionStack stack) {
        if (delayedHideForm) {
            ServerLoggers.remoteLifeLog("FORM DELAYED HIDE : " + this);
            try {
                form.syncLikelyOnClose(false, stack);
            } catch (SQLException | SQLHandledException e) {
                throw Throwables.propagate(e);
            }
            deactivateAndCloseLater(true);
        }

        return new ServerResponse(requestIndex, pendingActions.toArray(new ClientAction[pendingActions.size()]), false);
    }

    private boolean delayedHideFormSent;

    private ServerResponse prepareRemoteChangesResponse(long requestIndex, List<ClientAction> pendingActions, boolean delayedGetRemoteChanges, boolean delayedHideForm, ExecutionStack stack) {
        if(delayedHideForm) {
            delayedHideFormSent = true;
        }

        if (numberOfFormChangesRequests.get() > 1 || delayedGetRemoteChanges) {
            return returnRemoteChangesResponse(requestIndex, pendingActions, delayedHideForm, stack);
        }

        byte[] formChanges = getFormChangesByteArray(stack);

        List<ClientAction> resultActions = new ArrayList<>();
        resultActions.add(new ProcessFormChangesClientAction(requestIndex, formChanges));

        resultActions.addAll(pendingActions);

        return returnRemoteChangesResponse(requestIndex, resultActions, delayedHideForm, stack);
    }

    public byte[] getFormChangesByteArray(ExecutionStack stack) {
        try {
            FormChanges formChanges;
            if(isDeactivated() || delayedHideFormSent) // formWillBeClosed
                formChanges = FormChanges.EMPTY;
            else
                formChanges = form.endApply(stack);

            if (logger.isTraceEnabled()) {
                formChanges.logChanges(form, logger);
            }

            return formChanges.serialize();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String generateInvocationSid(long requestIndex) {
        String invocationSID;
        if (ServerLoggers.isPausableLogEnabled()) {
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

    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object[] actionResults) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, new Callable<ServerResponse>() {
            @Override
            public ServerResponse call() throws Exception {
                return currentInvocation.resumeAfterUserInteraction(actionResults);
            }
        });
    }

    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Throwable clientThrowable) throws RemoteException {
        return continueInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, new Callable<ServerResponse>() {
            @Override
            public ServerResponse call() throws Exception {
                return currentInvocation.resumeWithThrowable(clientThrowable);
            }
        });
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        boolean isInServerInvocation = currentInvocation != null;
        Object recentResult = recentResults.get(requestIndex).get();
        assert recentResult instanceof ServerResponse && isInServerInvocation == ((ServerResponse) recentResult).resumeInvocation;
        return isInServerInvocation;
    }

    private ServerResponse continueInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Callable<ServerResponse> continueRequest) throws RemoteException {
        if (continueIndex != -1) {
            Integer cachedContinueIndex = requestsContinueIndices.get(requestIndex);
            if (cachedContinueIndex != null && cachedContinueIndex == continueIndex) {
                Optional<?> result = recentResults.get(requestIndex);
                ServerLoggers.pausableLog("Return cachedResult for continue: rq#" + requestIndex + "; cont#" + continueIndex);
                return optionalResult(result);
            }
            if (cachedContinueIndex == null) {
                cachedContinueIndex = -1;
            }

            //следующий continue может прийти только, если был получен предыдущий
            assert continueIndex == cachedContinueIndex + 1;
        }

        assert requestIndex == -1 || currentInvocation.getRequestIndex() == requestIndex;

        if (continueIndex != -1) {
            requestsContinueIndices.put(requestIndex, continueIndex);
        }

        return callAndCacheResult(requestIndex, lastReceivedRequestIndex, continueRequest);
    }

    public void delayUserInteraction(ClientAction action) {
        currentInvocation.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return currentInvocation.pauseForUserInteraction(actions);
    }

    public void disconnect() throws SQLException, SQLHandledException {
        form.refreshData();
        if (currentInvocation != null && currentInvocation.isPaused()) {
            try {
                currentInvocation.cancel();
            } catch (Exception e) {
                logger.warn("Exception was thrown, while invalidating form", e);
            }
        }
    }

    public Object[] getImmutableMethods() {
        try {
            return new Object[]{getUserPreferences(), getColorPreferences(), getRichDesignByteArray(), getInitFilterPropertyDraw()};
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    protected String notSafeToString() {
        return "RF[" + form + "]";
    }


    // будем считать что если unreferenced \ finalized то форма точно также должна закрыться ???
    @Override
    protected void onClose() {
        try {
            form.explicitClose();
        } catch (Throwable t) {
            ServerLoggers.sqlSuppLog(t);
        }

        super.onClose();

        // важно делать после, чтобы закрытие navigator'а (а значит и sql conection'а) было после того как закрылись все формы
        RemoteFormListener listener = getRemoteFormListener();
        if (listener != null) {
            listener.formClosed(this);
        }
    }
    
    private static class SyncExecution {
        private boolean executed;
    }

    @Aspect
    public static class RemoteFormExecutionAspect {
        private Object syncExecute(Map<Long, SyncExecution> syncMap, long requestIndex, ProceedingJoinPoint joinPoint) throws Throwable {
            boolean needToWait = true;
            SyncExecution obj;
            Object result;

            synchronized (syncMap) {
                obj = syncMap.get(requestIndex);
                if (obj == null) { // this thread will do the calculation
                    obj = new SyncExecution();
                    syncMap.put(requestIndex, obj);
                    needToWait = false;
                }
            }

            if(needToWait) {
                synchronized (obj) {
                    while(!obj.executed)
                        obj.wait();
                }
            }

            try {
                result = joinPoint.proceed();
            } finally {
                if(!needToWait) {
                    synchronized (obj) {
                        obj.executed = true;
                        obj.notifyAll();
                    }
                    synchronized (syncMap) {
                        syncMap.remove(requestIndex);
                    }
                }
            }

            return result;
        }
        
        @Around("execution(private lsfusion.interop.action.ServerResponse RemoteForm.executeServerInvocation(long, long, RemotePausableInvocation)) && target(form) && args(requestIndex, lastReceivedRequestIndex, invocation)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteForm form, long requestIndex, long lastReceivedRequestIndex, RemotePausableInvocation invocation) throws Throwable {
            return syncExecute(form.syncExecuteServerInvocationMap, requestIndex, joinPoint);
        }

        @Around("execution(public lsfusion.interop.action.ServerResponse RemoteForm.continueServerInvocation(long, long, int, Object[])) && target(form) && args(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteForm form, long requestIndex, long lastReceivedRequestIndex, int continueIndex, final Object[] actionResults) throws Throwable {
            return syncExecute(form.syncContinueServerInvocationMap, requestIndex, joinPoint);
        }
        @Around("execution(public lsfusion.interop.action.ServerResponse RemoteForm.throwInServerInvocation(long, long, int, Throwable)) && target(form) && args(requestIndex, lastReceivedRequestIndex, continueIndex, throwable)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteForm form, long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable throwable) throws Throwable {
            return syncExecute(form.syncThrowInServerInvocationMap, requestIndex, joinPoint);
        }

        @Around("execution(private Object RemoteForm.processRMIRequest(long, long, EExecutionStackCallable)) && target(form) && args(requestIndex, lastReceivedRequestIndex, request)")
        public Object execute(ProceedingJoinPoint joinPoint, RemoteForm form, long requestIndex, long lastReceivedRequestIndex, EExecutionStackCallable request) throws Throwable {
            return syncExecute(form.syncProcessRMIRequestMap, requestIndex, joinPoint);
        }
    }

    @Override
    public Object getProfiledObject() {
        return form.entity;
    }
}
