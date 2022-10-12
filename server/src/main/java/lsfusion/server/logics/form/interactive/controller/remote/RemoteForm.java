package lsfusion.server.logics.form.interactive.controller.remote;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.*;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.event.FormEvent;
import lsfusion.interop.form.event.FormEventClose;
import lsfusion.interop.form.event.FormScheduler;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.order.user.Order;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.base.controller.remote.RemoteRequestObject;
import lsfusion.server.base.controller.thread.SyncType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.thread.ThreadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.EExecutionStackCallable;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.form.interactive.action.async.*;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.controller.context.RemoteFormContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.InteractiveFormReportManager;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupColumn;
import lsfusion.server.logics.form.interactive.instance.object.GroupMode;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.logics.form.interactive.property.Async;
import lsfusion.server.logics.form.stat.FormDataManager;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.StaticExportData;
import lsfusion.server.logics.form.stat.struct.export.plain.csv.ExportCSVAction;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.interop.action.ServerResponse.CHANGE;

// фасад для работы с клиентом
public class RemoteForm<F extends FormInstance> extends RemoteRequestObject implements RemoteFormInterface {
    private final static Logger logger = ServerLoggers.remoteLogger;

    public final F form;
    private final FormView richDesign;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    public RemoteForm(F form, int port, RemoteFormListener remoteFormListener, ExecutionStack upStack) throws RemoteException {
        super(port, upStack, form.entity.getSID(), form.isSync() ? SyncType.SYNC : SyncType.NOSYNC);

        setContext(new RemoteFormContext<>(this));
        this.form = form;
        this.richDesign = form.entity.getRichDesign();

        this.weakRemoteFormListener = new WeakReference<>(remoteFormListener);
        createPausablesExecutor();

        remoteFormListener.formCreated(this);
    }

    public RemoteFormListener getRemoteFormListener() {
        return weakRemoteFormListener.get();
    }

    public Object getGroupReportData(long requestIndex, long lastReceivedRequestIndex, final Integer groupId, final FormPrintType printType, final FormUserPreferences userPreferences) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("getReportData Action. GroupID: %s", groupId));
            }

            InteractiveFormReportManager formReportManager = new InteractiveFormReportManager(form, groupId, userPreferences);
            ReportGenerationData reportGenerationData = formReportManager.getReportData(printType);

            int minSizeForExportToCSV = Settings.get().getMinSizeForReportExportToCSV();
            if(minSizeForExportToCSV >= 0 && reportGenerationData.reportSourceData.length > minSizeForExportToCSV) {
                FormDataManager.ExportResult exportData = formReportManager.getExportData(0);
                RawFileData file = new ExportCSVAction(null, formReportManager.getFormEntity(), ListFact.EMPTY(), ListFact.EMPTY(), SetFact.EMPTYORDER(), SetFact.EMPTY(),
                        FormIntegrationType.CSV, null, 0, "UTF-8", false, ";", false, true).exportReport(new StaticExportData(exportData.keys, exportData.properties), exportData.hierarchy);
                return new RawFileData(ArrayUtils.addAll(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf}, file.getBytes())); //add bom bytes

            } else {
                return reportGenerationData;
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

    @Override
    public Set<Integer> getInputGroupObjects() {
        Set<Integer> inputObjects = new HashSet<>();
        if(form.inputObjects != null) {
            for (ObjectEntity objectEntity : form.inputObjects) {
                inputObjects.add(objectEntity.groupTo.ID);
            }
        }
        return inputObjects;
    }

    /**
     * этот метод не имеет специальной обработки RMI-вызова, т.к. предполагается, что он отработаывает как ImmutableMethod через createAndExecute
     */
    public FormUserPreferences getUserPreferences() throws RemoteException {

        FormUserPreferences result = form.loadUserPreferences();
        
        if (logger.isDebugEnabled()) {
            logger.debug("getUserPreferences Action");
        }
        
        return result;
    }

    public ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, final int groupID, final Integer pageSize) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            if (logger.isDebugEnabled()) {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
                logger.debug(String.format("changePageSize: [ID: %1$d]", groupObject.getID()));
                logger.debug(String.format("new page size %s:", pageSize));
            }
            
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
            form.changePageSize(groupObject, pageSize);
        });
    }

    public ServerResponse gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug("gainedFocus Action");
            }
            
            form.gainedFocus(stack);
        });
    }

    public ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, final boolean refresh, boolean forceLocalEvents) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            if (refresh) {
                form.formRefresh();
            }
        }, forceLocalEvents);
    }

    private static byte[] serializeAsyncs(Async[] asyncs) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            serializeAsyncs(asyncs, new DataOutputStream(outStream));
            return outStream.toByteArray();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static void serializeAsyncs(Async[] asyncs, DataOutputStream dataStream) throws IOException {
        dataStream.writeInt(asyncs.length);
        for(Async async : asyncs)
            async.serialize(dataStream);
    }

    private ImMap<ObjectInstance, DataObject> deserializeDataKeysValues(byte[] keysArray) throws IOException {
        return DataObject.assertDataObjects(deserializeKeysValues(keysArray));
    }
    private ImMap<ObjectInstance, ObjectValue> deserializeKeysValues(byte[] keysArray) throws IOException {
        return deserializeKeysValues(new DataInputStream(new ByteArrayInputStream(keysArray)), form);
    }
    // actually in most cases there can be only DataObjects, however when getting full keys (getFullKey on client - current objects of all groups), there might be some nulls, so it's better to check this case too
    public static ImMap<ObjectInstance, ObjectValue> deserializeKeysValues(DataInputStream inStream, FormInstance form) throws IOException {
        MExclMap<ObjectInstance, ObjectValue> mMapValues = MapFact.mExclMap();
        int cnt = inStream.readInt();
        for (int i = 0 ; i < cnt; ++i) {
            ObjectInstance object = form.getObjectInstance(inStream.readInt());
            mMapValues.exclAdd(object, FormChanges.deserializeObjectValue(inStream, object.getBaseClass()));
        }
        return mMapValues.immutable();
    }

    private static ImMap<ObjectInstance, ObjectValue> getObjectValues(DataSession session, ImMap<ObjectInstance, Object> dataKeys) throws SQLException, SQLHandledException {
        ImFilterValueMap<ObjectInstance, ObjectValue> mvKeys = dataKeys.mapFilterValues();
        for (int i=0,size=dataKeys.size();i<size;i++) {
            Object value = dataKeys.getValue(i);
            if (value != null)
                mvKeys.mapValue(i, session.getObjectValue(dataKeys.getKey(i).getBaseClass(), value));
        }
        return mvKeys.immutableValue();
    }

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupID, final byte[] value) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);

            ImMap<ObjectInstance, ? extends ObjectValue> valueToSet = deserializeKeysValues(value);
            if(valueToSet == null)
                return;

            groupObject.change(form.session, valueToSet, form, stack);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("changeGroupObject: [ID: %1$d]", groupObject.getID()));
                if (logger.isTraceEnabled()) {
                    logger.trace("   keys: ");
                    for (int i = 0, size = valueToSet.size(); i < size; i++) {
                        logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                    }
                }
            }
        });
    }

    public ServerResponse expandGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, final int groupId, boolean current) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);

            if (logger.isDebugEnabled())
                logger.debug(String.format("expandGroupObjectRecursive: [ID: %1$d]", group.getID()));

            group.expandCollapseAll(form, current, true);
        });
    }

    public ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);
            ImMap<ObjectInstance, DataObject> valueToSet = deserializeDataKeysValues(groupValues);
            if(valueToSet == null)
                return;

            if (logger.isDebugEnabled()) {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupId);
                logger.debug(String.format("expandGroupObject: [ID: %1$d]", groupObject.getID()));
                if (logger.isTraceEnabled()) {
                    logger.trace("   keys: ");
                    for (int i = 0, size = valueToSet.size(); i < size; i++) {
                        logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                    }
                }
            }
            group.expandCollapseDown(form, valueToSet, true);
        });
    }

    public ServerResponse collapseGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, final int groupId, boolean current) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);

            if (logger.isDebugEnabled()) {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupId);
                logger.debug(String.format("collapseGroupObjectRecursive: [ID: %1$d]", groupObject.getID()));
            }
            group.expandCollapseAll(form, current, false);
        });
    }

    public ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupId, final byte[] groupValues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);
            ImMap<ObjectInstance, DataObject> valueToSet = deserializeDataKeysValues(groupValues);
            if(valueToSet == null)
                return;

            if (logger.isDebugEnabled()) {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupId);
                logger.debug(String.format("collapseGroupObject: [ID: %1$d]", groupObject.getID()));
                if (logger.isTraceEnabled()) {
                    logger.trace("   keys: ");
                    for (int i = 0, size = valueToSet.size(); i < size; i++) {
                        logger.trace(String.format("     %1$s == %2$s", valueToSet.getKey(i), valueToSet.getValue(i)));
                    }
                }
            }
            group.collapse(form.session, valueToSet);
        });
    }

    public ServerResponse moveGroupObject(long requestIndex, long lastReceivedRequestIndex, final int parentGroupId, final byte[] parentKey, final int childGroupId, final byte[] childKey, final int index) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance parentGroup = form.getGroupObjectInstance(parentGroupId);
            GroupObjectInstance childGroup = form.getGroupObjectInstance(childGroupId);
            //todo:
//            form.moveGroupObject(parentGroup, deserializeGroupObjectKeys(parentGroup, parentKey));
        });
    }

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, final int groupID, final byte changeType) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("changePageSize: [ID: %1$d]", groupObject.getID()));
                logger.debug(String.format("new type: %s", changeType));
            }
            
            form.changeGroupObject(groupObject, Scroll.deserialize(changeType));
        });
    }

    @Override
    public ServerResponse changeMode(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, boolean setGroup, int[] propertyIDs, byte[][] columnKeys, int aggrProps, PropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, UpdateMode updateMode, ListViewType listViewType) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupObjectID);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("changeMode: [ID: %1$d]", groupObject.getID()));
            }

            if(setGroup) { // should correspond FormInstance constructor
                GroupMode setGroupMode = null;
                if(propertyIDs != null) {
                    MExclSet<GroupColumn> mGroupProps = SetFact.mExclSet(propertyIDs.length);
                    MExclSet<GroupColumn> mAggrProps = SetFact.mExclSet(propertyIDs.length);
                    for (int i = 0; i < propertyIDs.length; i++) {
                        PropertyDrawInstance property = form.getPropertyDraw(propertyIDs[i]);
                        GroupColumn column = new GroupColumn(property, deserializeDataKeysValues(columnKeys[i]));
                        if (i >= aggrProps)
                            mAggrProps.exclAdd(column);
                        else
                            mGroupProps.exclAdd(column);
                    }
                    setGroupMode = GroupMode.create(mGroupProps.immutable(), mAggrProps.immutable(), aggrType, form.instanceFactory);
                }
                groupObject.changeGroupMode(setGroupMode);
            }
            
            if(pageSize != null)
                groupObject.setPageSize(pageSize < 0 ? Settings.get().getPageSizeDefaultValue() : pageSize);
            
            if(forceRefresh)
                groupObject.forceRefresh();

            if(updateMode != null)
                groupObject.setUpdateMode(updateMode);

            if(listViewType != null)
                form.changeListViewType(groupObject, listViewType);
        });
    }

    public ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, final List<Integer> propertyIDs, final List<byte[]> columnKeys, final List<List<byte[]>> values) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            List<PropertyDrawInstance> properties = new ArrayList<>();
            List<ImMap<ObjectInstance, DataObject>> keys = new ArrayList<>();
            for (int i =0; i < propertyIDs.size(); i++) {
                PropertyDrawInstance<?> property = form.getPropertyDraw(propertyIDs.get(i));
                properties.add(property);
                keys.add(deserializeDataKeysValues(columnKeys.get(i)));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("pasteExternalTable Action");

                if (logger.isTraceEnabled()) {
                    for (int i = 0; i < propertyIDs.size(); i++) {
                        logger.trace(String.format("%s-%s", form.getPropertyDraw(propertyIDs.get(i)).getSID(), String.valueOf(columnKeys.get(i))));
                    }
                }
            }
            
            form.pasteExternalTable(properties, keys, values, stack);
        });
    }

    public ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, final Map<Integer, List<byte[]>> bkeys, final Map<Integer, byte[]> bvalues) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            Map<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> keysValues
                    = new HashMap<>();

            if (logger.isDebugEnabled())
                logger.debug("pasteMultiCellValue Action");
            
            for (Map.Entry<Integer, List<byte[]>> e : bkeys.entrySet()) {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(e.getKey());
                Object propValue = deserializeObject(bvalues.get(e.getKey()));

                MOrderMap<ImMap<ObjectInstance, DataObject>, Object> propKeys = MapFact.mOrderMap();
                for (byte[] bkey : e.getValue()) {
                    
                    if(logger.isDebugEnabled())
                        logger.debug(String.format("propertyDraw: %s", propertyDraw.getSID()));

                    propKeys.add(deserializeDataKeysValues(bkey), propValue);
                }

                keysValues.put(propertyDraw, propKeys.immutableOrder());
            }

            form.pasteMulticellValue(keysValues, stack);
        });
    }

    public ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte modiType, final byte[] columnKeys) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
            if(propertyDraw != null) {
                ImMap<ObjectInstance, ? extends ObjectValue> keys = deserializeKeysValues(columnKeys);

                Order order = Order.deserialize(modiType);

                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("changePropertyOrder: [ID: %1$d]", propertyID));
                    logger.debug(String.format("new order: %s", order.toString()));
                }

                propertyDraw.toDraw.changeOrder(propertyDraw.getDrawInstance().getRemappedPropertyObject(keys), Order.deserialize(modiType));
            }
        });
    }

    public ServerResponse setPropertyOrders(long requestIndex, long lastReceivedRequestIndex, final int groupObjectID,
                                            List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupObjectID);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("setPropertyOrders: [ID: %1$d]", groupObject.getID()));
            }

            groupObject.clearOrders();

            for(int i = 0; i < propertyList.size(); i++) {
                Integer propertyID = propertyList.get(i);
                byte[] columnKeys = columnKeyList.get(i);
                Boolean order = orderList.get(i);
                PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
                if(propertyDraw != null) { //can be set by userPreferences but hidden by security policy
                    ImMap<ObjectInstance, ObjectValue> keys = deserializeKeysValues(columnKeys);
                    PropertyObjectInstance property = propertyDraw.getDrawInstance().getRemappedPropertyObject(keys);
                    propertyDraw.toDraw.changeOrder(property, Order.ADD);
                    if(!order)
                        propertyDraw.toDraw.changeOrder(property, Order.DIR);
                }
            }

        });
    }

    public int countRecords(long requestIndex, long lastReceivedRequestIndex, final int groupObjectID) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            int result = form.countRecords(groupObjectID);

            if (logger.isDebugEnabled()) {
                GroupObjectInstance groupObject = form.getGroupObjectInstance(groupObjectID);
                logger.debug(String.format("countRecords Action. GroupObjectID: %s. Result: %s", groupObject.getID(), result));
            }

            return result;
        });
    }

    public Object calculateSum(long requestIndex, long lastReceivedRequestIndex, final int propertyID, final byte[] columnKeys) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
            ImMap<ObjectInstance, ? extends ObjectValue> keys = deserializeKeysValues(columnKeys);

            Object result = form.calculateSum(propertyDraw, keys);
            
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("calculateSum Action. propertyDrawID: %s. Result: %s", propertyDraw.getSID(), result));
            }
            
            return result;
        });
    }

    public byte[] groupData(long requestIndex, long lastReceivedRequestIndex, final Map<Integer, List<byte[]>> groupMap, final Map<Integer, List<byte[]>> sumMap,
                                                     final Map<Integer, List<byte[]>> maxMap, final boolean onlyNotNull) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            List<Map<Integer, List<byte[]>>> inMaps = Arrays.asList(groupMap, sumMap, maxMap);
            List<ImOrderMap<Object, ImList<ImMap<ObjectInstance, DataObject>>>> outMaps = new ArrayList<>();
            for (Map<Integer, List<byte[]>> one : inMaps) {
                MOrderExclMap<Object, ImList<ImMap<ObjectInstance, DataObject>>> mOutMap = MapFact.mOrderExclMap(one.size());
                for (Map.Entry<Integer, List<byte[]>> oneEntry : one.entrySet()) {
                    PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(oneEntry.getKey());
                    MList<ImMap<ObjectInstance, DataObject>> mList = ListFact.mList();
                    if (propertyDraw != null) {
                        for (byte[] columnKeys : oneEntry.getValue()) {
                            mList.add(deserializeDataKeysValues(columnKeys));
                        }
                        mOutMap.exclAdd(propertyDraw, mList.immutableList());
                    } else
                        mOutMap.exclAdd(0, ListFact.EMPTY());
                }
                outMaps.add(mOutMap.immutableOrderCopy());
            }

            if (logger.isDebugEnabled()) {
                logger.debug("groupData Action");
            }

            Map<List<Object>, List<Object>> grouped = form.groupData(BaseUtils.immutableCast(outMaps.get(0)),
                    outMaps.get(1), BaseUtils.immutableCast(outMaps.get(2)), onlyNotNull);

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
        });
    }

    @Override
    public List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, final String groupObjectSID) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("readGroupings Action. GroupObjectSID: %s", groupObjectSID));
            }
            return form.readGroupings(groupObjectSID);
        });
    }

    @Override
    public void saveGrouping(long requestIndex, long lastReceivedRequestIndex, final FormGrouping grouping) throws RemoteException {
        processRMIRequest(requestIndex, lastReceivedRequestIndex, (EExecutionStackCallable<Void>) stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("readGroupings Action: [ID: %s]", grouping.groupObjectSID));
            }
            
            form.saveGrouping(grouping, stack);
            return null;
        });
    }

    public ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, final byte[][] filters) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            for (GroupObjectInstance group : form.getGroups()) {
                group.clearUserFilters();
            }
            for (byte[] state : filters) {
                FilterInstance filter = FilterInstance.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
                GroupObjectInstance applyObject = filter.getApplyObject();
                if(applyObject != null) {
                    applyObject.addUserFilter(filter);
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("set user filter: [CLASS: %1$s]", filter.getClass()));
                        logger.debug(String.format("apply object: %s", filter.getApplyObject().getID()));
                    }
                }
            }
        });
    }

    public ServerResponse setRegularFilter(long requestIndex, long lastReceivedRequestIndex, final int groupID, final int filterID) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            form.setRegularFilter(form.getRegularFilterGroup(groupID), filterID);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("set regular filter: [GROUP: %1$s]", groupID));
                logger.debug(String.format("filter ID: %s", filterID));
            }
        });
    }

    public ServerResponse setViewFilters(long requestIndex, long lastReceivedRequestIndex, final byte[][] filters, int pageSize) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            Set<FilterInstance> filtersInstances = new HashSet<>();
            GroupObjectInstance applyObject = null;
            for (byte[] state : filters) {
                FilterInstance filter = FilterInstance.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
                applyObject = filter.getApplyObject();
                filtersInstances.add(filter);
            }
            if (applyObject != null) {
                applyObject.setViewFilters(filtersInstances);
                applyObject.setPageSize(pageSize);
            }
        });
    }

    public String getCanonicalName() {
        return form.entity.getCanonicalName();
    }

    @Override
    public ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, FormEvent formEvent, byte[] pushAsyncResult) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug("executeEventAction");
            }

            AsyncEventExec asyncEventExec = richDesign.getAsyncEventExec(formEvent);
            form.fireFormEvent(stack, formEvent, asyncEventExec != null ? asyncEventExec.deserializePush(pushAsyncResult) : null);
        });
    }

    public ServerResponse setTabActive(long requestIndex, long lastReceivedRequestIndex, final int tabPaneID, final int childId) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug("setTabVisible Action");
            }
            
            form.setTabVisible((ContainerView) richDesign.findById(tabPaneID), richDesign.findById(childId));
        });
    }

    @Override
    public ServerResponse setContainerCollapsed(long requestIndex, long lastReceivedRequestIndex, int containerID, boolean collapsed) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug("setContainerCollapsed Action");
            }

            form.setContainerCollapsed((ContainerView) richDesign.findById(containerID), collapsed);
        });
    }

    @Override
    public ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, final GroupObjectUserPreferences preferences, final boolean forAllUsers, final boolean completeOverride, final String[] hiddenProps) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {

            if (logger.isDebugEnabled()) {
                logger.debug("saveUserPreferences Action");
            }
            
            form.saveUserPreferences(stack, preferences, forAllUsers, completeOverride);
            
            form.refreshUPHiddenProperties(preferences.groupObjectSID, hiddenProps);
        });
    }

    @Override
    public ServerResponse refreshUPHiddenProperties(long requestIndex, long lastReceivedRequestIndex, final String groupObjectSID, final String[] propSids) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> form.refreshUPHiddenProperties(groupObjectSID, propSids));
    }

    @Override
    public ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, String actionSID, int[] propertyIDs, byte[][] fullKeys, boolean[] externalChanges, byte[][] pushAsyncResults) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            for (int j = 0; j < propertyIDs.length; j++) {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyIDs[j]);
                ImMap<ObjectInstance, ? extends ObjectValue> keys = deserializeKeysValues(fullKeys[j]);

                Function<AsyncEventExec, PushAsyncResult> asyncResult = null;
                byte[] pushAsyncResult = pushAsyncResults[j];
                if(pushAsyncResult != null)
                    asyncResult = asyncEventExec -> asyncEventExec.deserializePush(pushAsyncResult);
                
                form.executeEventAction(propertyDraw, actionSID, keys, externalChanges[j], asyncResult, stack);

                logger.info(String.format("executeEventAction: [ID: %1$d, SID: %2$s]", propertyDraw.getID(), propertyDraw.getSID()));
                if (logger.isTraceEnabled()) {
                    if (keys.size() > 0) {
                        logger.trace("   columnKeys: ");
                        for (int i = 0, size = keys.size(); i < size; i++) {
                            logger.trace(String.format("     %1$s == %2$s", keys.getKey(i), keys.getValue(i)));
                        }
                    }
                    logger.trace("   current object's values: ");
                    for (ObjectInstance obj : form.getObjects()) {
                        logger.trace(String.format("     %1$s == %2$s", obj, obj.getObjectValue()));
                    }
                }
            }
        });
    }

    @Override
    public byte[] getAsyncValues(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID, String value, int asyncIndex) throws RemoteException {
        try {
            // we're setting cacelable thread we're sure that global branch is used and it can be canceled without consequences
            // however in some cases we might wanna cancel pessimistic requests too (when statement supports that), but it may cause some troubles because there is no transaction, so the consequences are unpredictable (plus it's pretty rare case)
            Supplier<Boolean> setCancelableThread = () -> {
                synchronized (asyncLock) {
                    assert asyncIndex <= asyncLastIndex;
                    if (asyncIndex == asyncLastIndex) {
                        asyncLastThread = Thread.currentThread();
                        return true;
                    }

                    return false;
                }
            };

            Result<String> actualValue = new Result<>(value);
            synchronized (asyncLock) { // we check asyncLastIndex even for pessimistic requests since we don't want useless requests
                if (asyncIndex >= asyncLastIndex) {
                    if (asyncLastThread != null)
                        ThreadUtils.interruptThread(getContext(), asyncLastThread);
                    asyncLastIndex = asyncIndex;
                } else
                    actualValue.set(null); // canceling async values query, but we still need to register that request (in processRMIRequest)
            }

            Async[] result;
            if(requestIndex >= 0)
                result = processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
                    // we need to recheck since asyncLastIndex can be updated and we don't need this query anymore (actualValue will be set to null)
                    synchronized (asyncLock) {
                        assert asyncIndex <= asyncLastIndex;
                        if (asyncIndex != asyncLastIndex)
                            actualValue.set(null);
                    }

                    return getAsyncValues(propertyID, fullKey, actionSID, actualValue.result, false, setCancelableThread);
                });
            else
                result = getAsyncValues(propertyID, fullKey, actionSID, actualValue.result, true, setCancelableThread);

            synchronized (asyncLock) {
                assert asyncIndex <= asyncLastIndex;
                if(asyncLastIndex == asyncIndex)
                    asyncLastThread = null;
            }

            if(result == null)
                return null;
            return serializeAsyncs(result);
        } catch (Throwable t) { // interrupted for example
//            if(ExceptionUtils.getRootCause(t) instanceof InterruptedException)
            Thread.interrupted(); // we want to reset interrupted state, otherwise RemoteExceptionsAspect will rethrow InterruptedException to the client, where it is not always ignored (for example getPessimisticValues)

            ServerLoggers.sqlSuppLog(t);
            return serializeAsyncs(new Async[] {Async.CANCELED});
//            throw Throwables.propagate(e);
        }
    }

    private Thread asyncLastThread;
    private int asyncLastIndex = 0;
    private final Object asyncLock = new Object();

    public Async[] getAsyncValues(int propertyID, byte[] fullKey, String actionSID, String value, Boolean optimistic, Supplier<Boolean> optimisticRun) throws SQLException, SQLHandledException, IOException {
        if(value == null)
            return new Async[] {Async.CANCELED};

        PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
        ImMap<ObjectInstance, ? extends ObjectValue> keys = deserializeKeysValues(fullKey);

        Async[] result = form.getAsyncValues(propertyDraw, keys, actionSID, value, optimistic, optimisticRun);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("getAsyncValues Action. propertyDrawID: %s. Result: %s", propertyDraw.getSID(), result));
        }

        return result;
    }

    public ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, final int idNotification) throws RemoteException {
        return processPausableRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            RemoteFormListener remoteNavigator = getRemoteFormListener();
            if(remoteNavigator != null) {
                remoteNavigator.executeNotificationAction(form, stack, idNotification);
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
            deactivateAndCloseLater(false);
        }

        return new ServerResponse(requestIndex, pendingActions.toArray(new ClientAction[pendingActions.size()]), false);
    }

    private boolean delayedHideFormSent;

    @Override
    protected ServerResponse prepareResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack, boolean forceLocalEvents) {
        return prepareRemoteChangesResponse(requestIndex, pendingActions, stack, forceLocalEvents);
    }

    private ServerResponse prepareRemoteChangesResponse(long requestIndex, List<ClientAction> pendingActions, ExecutionStack stack, boolean forceLocalEvents) {
        boolean delayedGetRemoteChanges = false;
        boolean delayedHideForm = false;
        for(ClientAction action : pendingActions) {
            delayedGetRemoteChanges = delayedGetRemoteChanges || action instanceof AsyncGetRemoteChangesClientAction;
            delayedHideForm = delayedHideForm || action instanceof HideFormClientAction;
        }        

        if(delayedHideForm) {
            delayedHideFormSent = true;
        }

        // the first check can cause some undesirable effects when we have sync call and after there is an async call
        // (for example there is difference in web and desktop client behaviour: DIALOG some change that requires a lot of time to update form, from which the call is made - then that form will have "gained focus" event, in web that call will be before continue, and in desktop after)
        // in that case sync call will get no data, and all the work async call will do the work (in upper example, in web there will be no busy dialog, and in desktop there will be oone)
        // however so far it doesn't seem to be a problem (to solve it we have to pass if the call is sync / async and delay getting remote changes only for async changes)
        if (numberOfFormChangesRequests.get() > 1 || delayedGetRemoteChanges) {
            return returnRemoteChangesResponse(requestIndex, pendingActions, delayedHideForm, stack);
        }

        List<ClientAction> resultActions = new ArrayList<>();

        byte[] formChanges = getFormChangesByteArray(stack, forceLocalEvents, resultActions);

        resultActions.add(new ProcessFormChangesClientAction(requestIndex, formChanges));

        resultActions.addAll(pendingActions);

        return returnRemoteChangesResponse(requestIndex, resultActions, delayedHideForm, stack);
    }

    public byte[] getFormChangesByteArray(ExecutionStack stack) {
        return getFormChangesByteArray(stack, false, new ArrayList<>());
    }

    public byte[] getFormChangesByteArray(ExecutionStack stack, boolean forceLocalEvents, List<ClientAction> resultActions) {
        try {
            FormChanges formChanges;
            if(isDeactivated() || delayedHideFormSent) // formWillBeClosed
                formChanges = FormChanges.EMPTY;
            else
                formChanges = form.getChanges(stack, forceLocalEvents, resultActions);

            if (logger.isDebugEnabled()) {
                logger.debug("getFormChanges");
                if (logger.isTraceEnabled()) {
                    formChanges.logChanges(form, logger);
                }
            }

            return formChanges.serialize();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void delayUserInteraction(ClientAction action, String message) {
        if(currentInvocationExternal) {
            if(action instanceof UpdateEditValueClientAction || action instanceof AsyncGetRemoteChangesClientAction) // in external CHANGE state is updated at once on a client, we don't need to reupdate it
                return;
            if(message != null) // we'll proceed this message in popLogMessage
                return;
        }
        super.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        if(currentInvocationExternal) { // temporary for formCancel
            Object[] result = new Object[actions.length];
            for (int i = 0; i < actions.length; i++) {
                ClientAction action = actions[i];
                if (action instanceof ConfirmClientAction)
                    result[i] = JOptionPane.YES_OPTION;
                else {
                    result = null;
                    break;
                }
            }
            if(result != null)
                return result;
        }
        return super.requestUserInteraction(actions);
    }

    public Object[] getImmutableMethods() {
        try {
            return new Object[]{getUserPreferences(), getRichDesignByteArray(), getInitFilterPropertyDraw(), getInputGroupObjects()};
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    protected String notSafeToString() {
        return "RF[" + form + "]";
    }
    
    public JSONObject getFormChangesExternal(ExecutionStack stack) {
        try {
            FormChanges formChanges;
            if(numberOfFormChangesRequests.get() > 1 || isDeactivated())
                formChanges = FormChanges.EMPTY;
            else
                formChanges = form.getChanges(stack);
            // should use formatJSON and getIntegrationSID
            // if group consists of one object and their sids are equal put value
            // if there are no gridObjects, we can use GroupObjectInstance.keys instead
            return formChanges.serializeExternal();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @IdentityLazy
    public FormEntity.MetaExternal getMetaExternal() {
        return form.entity.getMetaExternal(form.securityPolicy);
    }

    private boolean currentInvocationExternal = false;

    @Override
    public Pair<Long, String> changeExternal(final long requestIndex, long lastReceivedRequestIndex, final String json) throws RemoteException {
        return processRMIRequest(requestIndex, lastReceivedRequestIndex, stack -> {
            assert !currentInvocationExternal;
            currentInvocationExternal = true;
            try {
                // parse json do changes (values should be passed as is, they are parsed inside particular change call)
                // if group is value (not an object), pass singleton map with group sid
                JSONObject modify = new JSONObject(json);

                // changing current object (before changing property to have relevant current objects)
                MExclMap<ObjectInstance, ObjectValue> mCurrentObjects = MapFact.mExclMap();
                Iterator<String> modifyKeys = modify.keys();
                while (modifyKeys.hasNext()) {
                    String modifyKey = modifyKeys.next();
                    Object modifyValue = modify.get(modifyKey);

                    if (modifyValue instanceof JSONObject) { // group objects
                        Object value = ((JSONObject) modifyValue).opt("value");
                        if (value != null) // in current js interface value is passed for all objects, but in theory it will work even when there are not all values
                            changeGroupObjectExternal(modifyKey, value, stack, mCurrentObjects);
                    }
                }
                ImMap<ObjectInstance, ObjectValue> currentObjects = mCurrentObjects.immutable();

                ThreadLocalContext.pushLogMessage();
                try {
                    modifyKeys = modify.keys();
                    while (modifyKeys.hasNext()) {
                        String groupObjectOrProperty = modifyKeys.next();
                        Object modifyValue = modify.get(groupObjectOrProperty);

                        if (modifyValue instanceof JSONObject) { // group objects
                            JSONObject groupObjectModify = (JSONObject) modifyValue;
                            Iterator<String> propertyKeys = groupObjectModify.keys();
                            while (propertyKeys.hasNext()) {
                                String propertyName = propertyKeys.next();
                                if (!propertyName.equals("value"))
                                    changePropertyOrExecActionExternal(groupObjectOrProperty, propertyName, groupObjectModify.get(propertyName), currentObjects, stack);
                            }
                        } else // properties without group
                            changePropertyOrExecActionExternal(null, groupObjectOrProperty, modifyValue, currentObjects, stack);
                    }
                } finally {
                    ImList<AbstractContext.LogMessage> logMessages = ThreadLocalContext.popLogMessage();
                    if(form.dataChanged) // just for optimization purposes (otherwise just any change property / exec action would do)
                        form.BL.LM.getLogMessage().change(DataSession.getLogMessage(logMessages, true), form);
                }

                return new Pair<>(requestIndex, getFormChangesExternal(stack).toString());
            } finally {
                currentInvocationExternal = false;
            }
        });
    }

    private static Object formatJSON(ObjectInstance object, ObjectValue value) {
        return formatJSONNull(object.getType(), value.getValue());
    }

    private static Object parseJSON(ObjectInstance object, Object value) throws ParseException {
        return object.getType().parseJSON(value);
    }

    private static boolean isSimpleGroup(GroupObjectInstance groupObject) {
        return groupObject.objects.size() == 1 && groupObject.getIntegrationSID().equals(groupObject.objects.single().getSID());        
    }

    // we need nulls in external interface to override not null values (while updating)
    public static Object formatJSONNull(Type type, Object value) {
        Object jsonValue = type.formatJSON(value);
        return jsonValue != null ? jsonValue : JSONObject.NULL;
    }

    public static Object formatJSON(GroupObjectInstance group, ImMap<ObjectInstance, ? extends ObjectValue> gridObjectRow) {
        if (isSimpleGroup(group)) {
            ObjectInstance object = group.objects.single();
            return formatJSON(object, gridObjectRow.get(object));
        }

        JSONObject result = new JSONObject();
        for (ObjectInstance object : group.objects)
            result.put(object.getSID(), formatJSON(object, gridObjectRow.get(object)));
        return result;
    }

    private static ImMap<ObjectInstance, Object> parseJSON(GroupObjectInstance group, Object values) throws ParseException {
        if(isSimpleGroup(group)) {
            ObjectInstance object = group.objects.single();
            return MapFact.singleton(object, parseJSON(object, values));
        }

        final JSONObject jsonObject = (JSONObject) values;
        ImValueMap<ObjectInstance, Object> mvResult = group.objects.mapItValues();// exception
        for(int i=0,size=group.objects.size();i<size;i++) {
            ObjectInstance object = group.objects.get(i);
            mvResult.mapValue(i, parseJSON(object, jsonObject.get(object.getSID())));
        }
        return mvResult.immutableValue();
    }

    private static ImMap<ObjectInstance, ? extends ObjectValue> parseJSON(GroupObjectInstance groupObject, DataSession session, Object values) throws ParseException, SQLException, SQLHandledException {
        ImMap<ObjectInstance, Object> valueObjects = parseJSON(groupObject, values);
        ImMap<ObjectInstance, ? extends ObjectValue> valueToSet = groupObject.findGroupObjectValue(valueObjects);
        if(valueToSet == null) { // group object is in panel or objects are not in grid (it's possible with external api) 
            valueToSet = getObjectValues(session, valueObjects);
            if(valueToSet.size() < valueObjects.size()) // if there are not all valueObjects, change group object to null (assertion in GroupObjectInstance.change requires this)
                valueToSet = MapFact.EMPTY();
        }
        return valueToSet;
    }

    private Pair<ObjectInstance, Boolean> getNewDeleteExternal(String groupSID, String propertySID) {
        GroupObjectInstance groupObject = form.getGroupObjectInstanceIntegration(groupSID);
        PropertyDrawInstance<?> propertyDraw = form.getPropertyDrawIntegration(groupSID, propertySID);

        FormEntity.MetaExternal metaExternal = getMetaExternal();
        Boolean newDelete = metaExternal.groups.get(groupObject.entity).props.get(propertyDraw.entity).newDelete;
        if(newDelete != null)
            return new Pair<>(groupObject.objects.single(), newDelete);
        return null;
    }

    private void changeGroupObjectExternal(String groupSID, Object values, ExecutionStack stack, MExclMap<ObjectInstance, ObjectValue> mCurrentObjects) throws ParseException, SQLException, SQLHandledException {
        GroupObjectInstance groupObject = form.getGroupObjectInstanceIntegration(groupSID);
        DataSession session = form.session;

        boolean change = true;
        if(values instanceof JSONArray) {
            change = false;
            values = ((JSONArray)values).get(0);
        }

        ImMap<ObjectInstance, ? extends ObjectValue> objectValues = parseJSON(groupObject, session, values);
        mCurrentObjects.exclAddAll(objectValues);
        if(change)// there is no addSeek so maybe we should use forceChangeObject instead of change
            groupObject.change(session, objectValues, form, stack);
    }

    private void changePropertyOrExecActionExternal(String groupSID, String propertySID, final Object value, ImMap<ObjectInstance, ? extends ObjectValue> currentObjects, ExecutionStack stack) throws SQLException, SQLHandledException, ParseException {
        PropertyDrawInstance propertyDraw = form.getPropertyDrawIntegration(groupSID, propertySID);

        Function<AsyncEventExec, PushAsyncResult> asyncResult = null;
        if(propertyDraw.isProperty()) {
            asyncResult = asyncEventExec -> {
                try {
                    return asyncEventExec instanceof AsyncInput ? new PushAsyncInput(ObjectValue.getValue(((AsyncInput) asyncEventExec).changeType.parseJSON(value), ((AsyncInput) asyncEventExec).changeType)) : null;
                } catch (ParseException e) {
                    throw Throwables.propagate(e);
                }
            };

            // it's tricky here, unlike changeGroupObject, changeProperty is cancelable, i.e. its change may be canceled, but there will be no undo change in getChanges
            // so there are 2 ways store previous values on client (just like it is done now on desktop and web-client, which is not that easy task), or just force that property reread
            // we'll try that approach on external api, if it works fine, maybe we'll change corresponding behaviour on desktop and web-client
            form.forcePropertyDrawUpdate(propertyDraw);
        } else {
            Pair<ObjectInstance, Boolean> newDelete;
            if(groupSID != null && (newDelete = getNewDeleteExternal(groupSID, propertySID)) != null) {
                if(newDelete.second)
                    asyncResult = asyncEventExec -> new PushAsyncAdd((DataObject) currentObjects.get(newDelete.first));

                // see comment above
                newDelete.first.groupTo.forceUpdateKeys();
            }
        }
        form.executeEventAction(propertyDraw, CHANGE, currentObjects, true, asyncResult, stack);
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
    
    @Override
    public Object getProfiledObject() {
        return form.entity;
    }
}
