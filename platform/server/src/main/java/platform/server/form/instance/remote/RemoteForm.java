package platform.server.form.instance.remote;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.server.Context;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.listener.RemoteFormListener;
import platform.server.form.view.FormView;
import platform.server.form.view.report.ReportDesignGenerator;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.Property;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.deserializeObject;
import static platform.server.logics.BusinessLogics.getCurrentActionMessage;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>, F extends FormInstance<T>> extends platform.interop.remote.RemoteObject implements RemoteFormInterface, Context {
    private final static Logger logger = Logger.getLogger(RemoteForm.class);

    public final F form;
    public final FormView richDesign;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    private RemoteFormListener getRemoteFormListener() {
        return weakRemoteFormListener.get();
    }

    public RemoteForm(F form, FormView richDesign, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        super(port);

        this.form = form;
        this.richDesign = richDesign;
        this.weakRemoteFormListener = new WeakReference<RemoteFormListener>(remoteFormListener);
        if (remoteFormListener != null) {
            remoteFormListener.formCreated(this);
        }
    }

    public byte[] getReportHierarchyByteArray() {
        Map<String, List<String>> dependencies = form.entity.getReportHierarchy().getReportHierarchyMap();
        return getReportHierarchyByteArray(dependencies);
    }

    public byte[] getSingleGroupReportHierarchyByteArray(int groupId) {
        Map<String, List<String>> dependencies = form.entity.getSingleGroupReportHierarchy(groupId).getReportHierarchyMap();
        return getReportHierarchyByteArray(dependencies);
    }

    private byte[] getReportHierarchyByteArray(Map<String, List<String>> dependencies) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            objOut.writeUTF(GroupObjectHierarchy.rootNodeName);
            objOut.writeObject(dependencies);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getReportDesignsByteArray(boolean toExcel) {
        return getReportDesignsByteArray(toExcel, null);
    }

    /// Отчет по одной группе
    public byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId) {
        return getReportDesignsByteArray(toExcel, groupId);
    }

    private byte[] getReportDesignsByteArray(boolean toExcel, Integer groupId) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            Map<String, JasperDesign> res = getReportDesigns(toExcel, groupId);
            objOut.writeObject(res);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Integer> getGridGroups(Integer groupId) {
        Set<Integer> gridGroupsId = new HashSet<Integer>();
        for (GroupObjectInstance group : form.groups) {
            if (group.curClassView == ClassViewType.GRID && (groupId == null || groupId == group.getID())) {
                gridGroupsId.add(group.getID());
            }
        }
        return gridGroupsId;
    }

    public byte[] getReportSourcesByteArray() {
        GroupObjectHierarchy.ReportHierarchy hierarchy = form.entity.getReportHierarchy();
        ReportSourceGenerator<T> sourceGenerator = new ReportSourceGenerator<T>(form, hierarchy, getGridGroups(null));
        return getReportSourcesByteArray(sourceGenerator);
    }

    public byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException {
        ReportSourceGenerator<T> sourceGenerator = new ReportSourceGenerator<T>(form, form.entity.getSingleGroupReportHierarchy(groupId),
                form.entity.getReportHierarchy(), getGridGroups(groupId));
        return getReportSourcesByteArray(sourceGenerator);
    }

    private byte[] getReportSourcesByteArray(ReportSourceGenerator<T> sourceGenerator) {
        try {
            Map<String, ReportData> sources = sourceGenerator.generate();
            ReportSourceGenerator.ColumnGroupCaptionsData columnGroupCaptions = sourceGenerator.getColumnGroupCaptions();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            dataStream.writeInt(sources.size());
            for (Map.Entry<String, ReportData> source : sources.entrySet()) {
                dataStream.writeUTF(source.getKey());
                source.getValue().serialize(dataStream);
            }

            int columnPropertiesCount = columnGroupCaptions.propertyObjects.size();
            dataStream.writeInt(columnPropertiesCount);

            serializePropertyObjects(dataStream, columnGroupCaptions.propertyObjects);

            dataStream.writeInt(columnGroupCaptions.data.size());
            for (Map.Entry<String, Map<List<Object>, Object>> entry : columnGroupCaptions.data.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                Map<List<Object>, Object> value = entry.getValue();
                dataStream.writeInt(value.size());
                for (Map.Entry<List<Object>, Object> valueEntry : value.entrySet()) {
                    for (Object obj : valueEntry.getKey()) {
                        BaseUtils.serializeObject(dataStream, obj);
                    }
                    BaseUtils.serializeObject(dataStream, valueEntry.getValue());
                }
            }

            serializePropertyObjects(dataStream, columnGroupCaptions.columnObjects);

            for (Map.Entry<String, LinkedHashSet<List<Object>>> entry : columnGroupCaptions.columnData.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                LinkedHashSet<List<Object>> value = entry.getValue();
                dataStream.writeInt(value.size());
                for (List<Object> list : value) {
                    for (Object obj : list) {
                        BaseUtils.serializeObject(dataStream, obj);
                    }
                }
            }

            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void serializePropertyObjects(DataOutputStream stream, Map<String, List<ObjectInstance>> objects) throws IOException {
        for (Map.Entry<String, List<ObjectInstance>> entry : objects.entrySet()) {
            stream.writeUTF(entry.getKey());
            stream.writeInt(entry.getValue().size());
            for (ObjectInstance object : entry.getValue()) {
                stream.writeInt(object.getID());
            }
        }
    }

    private GroupObjectHierarchy.ReportHierarchy getReportHierarchy(Integer groupId) {
        if (groupId == null) {
            return form.entity.getReportHierarchy();
        } else {
            return form.entity.getSingleGroupReportHierarchy(groupId);
        }
    }

    private Map<String, JasperDesign> getCustomReportDesigns(boolean toExcel, Integer groupId) {
        try {
            GroupObjectHierarchy.ReportHierarchy hierarchy = getReportHierarchy(groupId);
            Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();
            List<String> ids = new ArrayList<String>();
            ids.add(GroupObjectHierarchy.rootNodeName);
            for (GroupObjectHierarchy.ReportNode node : hierarchy.getAllNodes()) {
                ids.add(node.getID());
            }
            for (String id : ids) {
                String resourceName = "/" + getCustomReportName(id, getReportSID(toExcel, groupId));
                InputStream iStream = getClass().getResourceAsStream(resourceName);
                // Если не нашли custom design для xls, пробуем найти обычный
                if (toExcel && iStream == null) {
                    resourceName = "/" + getCustomReportName(id, getReportSID(false, groupId));
                    iStream = getClass().getResourceAsStream(resourceName);
                }
                if (iStream == null) {
                    return null;
                }
                JasperDesign subreport = JRXmlLoader.load(iStream);
                designs.put(id, subreport);
            }
            return designs;
        } catch (Exception e) {
            return null;
        }
    }

    private static final String xlsSuffix = "_xls";
    private static final String tablePrefix = "_Table";

    private String getReportSID(boolean toExcel, Integer groupId) {
        String reportSID = getSID() + (groupId == null ? "" : tablePrefix + form.getGroupObjectInstance(groupId).getSID());
        return reportSID + (toExcel ? xlsSuffix : "");
    }

    private Map<String, JasperDesign> getReportDesigns(boolean toExcel, Integer groupId) {
        String sid = getReportSID(toExcel, groupId);
        Map<String, JasperDesign> customDesigns = getCustomReportDesigns(toExcel, groupId);
        if (customDesigns != null) {
            return customDesigns;
        }

        Set<Integer> hidedGroupsId = new HashSet<Integer>();
        for (GroupObjectInstance group : form.groups) {
            if (group.curClassView == ClassViewType.HIDE || groupId != null && groupId != group.getID()) {
                hidedGroupsId.add(group.getID());
            }
        }
        try {
            ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId), hidedGroupsId, toExcel);
            Map<String, JasperDesign> designs = generator.generate();
            for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
                String id = entry.getKey();
                String reportName = getAutoReportName(id, sid);

                new File(reportName).getParentFile().mkdirs();

                JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            }
            return designs;
        } catch (JRException e) {
            throw new RuntimeException(ServerResourceBundle.getString("form.instance.error.creating.design"), e);
        }
    }

    private String getCustomReportName(String name, String sid) {
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            return "reports/custom/" + sid + ".jrxml";
        } else {
            return "reports/custom/" + sid + "_" + name + ".jrxml";
        }
    }

    private String getAutoReportName(String name, String sid) {
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            return "reports/auto/" + sid + ".jrxml";
        } else {
            return "reports/auto/" + sid + "_" + name + ".jrxml";
        }
    }

    @Aspect
    private static class RemoteFormContextHoldingAspect {
        @Before("execution(* platform.interop.form.RemoteFormInterface.*(..)) && target(remoteForm)")
        public void beforeCall(RemoteForm remoteForm) {
            Context.context.set(remoteForm);
        }
    }

    public String getRemoteActionMessage() {
        return getCurrentActionMessage();
    }

    public BusinessLogics.MessageStack actionMessageStack = new BusinessLogics.MessageStack();

    public String getActionMessage() {
        return actionMessageStack.getMessage();
    }

    public void setActionMessage(String message) {
        actionMessageStack.set(message);
    }

    public void pushActionMessage(String segment) {
        actionMessageStack.push(segment);
    }

    public String popActionMessage() {
        return actionMessageStack.pop();
    }

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

    public void changePageSize(int groupID, Integer pageSize) throws RemoteException {

        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
        form.changePageSize(groupObject, pageSize);
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    private byte[] getFormChangesByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            FormChanges formChanges = form.endApply();
            formChanges.serialize(new DataOutputStream(outStream));

            if (logger.isDebugEnabled()) {
                formChanges.logChanges(form, logger);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outStream.toByteArray();
    }

    public RemoteChanges getRemoteChanges() {
        byte[] formChanges = getFormChangesByteArray();

        List<ClientAction> remoteActions = actions;
        actions = new ArrayList<ClientAction>();

        int objectClassID = 0;
        if (updateCurrentClass != null) {
            ConcreteCustomClass currentClass = form.getObjectClass(updateCurrentClass);
            RemoteFormListener remoteFormListener = getRemoteFormListener();
            if (currentClass != null && remoteFormListener != null && remoteFormListener.currentClassChanged(currentClass)) {
                objectClassID = currentClass.ID;
            }

            updateCurrentClass = null;
        }
        return new RemoteChanges(formChanges, remoteActions, objectClassID);
    }


    private Map<ObjectInstance, Object> deserializeKeysValues(byte[] keysArray) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(keysArray));

        Map<ObjectInstance, Object> mapValues = new HashMap<ObjectInstance, Object>();
        int cnt = inStream.readInt();
        for (int i = 0 ; i < cnt; ++i) {
            mapValues.put(form.getObjectInstance(inStream.readInt()), deserializeObject(inStream));
        }

        return mapValues;
    }

    private Map<ObjectInstance, DataObject> deserializePropertyKeys(PropertyDrawInstance<?> propertyDraw, byte[] columnKeys) throws IOException {
        Map<ObjectInstance, DataObject> keys = new HashMap<ObjectInstance, DataObject>();
        Map<ObjectInstance, Object> dataKeys = deserializeKeysValues(columnKeys);

        for (GroupObjectInstance groupInstance : propertyDraw.columnGroupObjects) {
            Map<ObjectInstance, DataObject> key = groupInstance.findGroupObjectValue(dataKeys);
            if (key != null) {
                keys.putAll(key);
            }
        }
        return keys;
    }

    private Map<ObjectInstance, DataObject> deserializeGroupObjectKeys(GroupObjectInstance group, byte[] treePathKeys) throws IOException {
        return group.findGroupObjectValue(deserializeKeysValues(treePathKeys));
    }

    public void changeGroupObject(int groupID, byte[] value) {

        try {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
            Map<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(groupObject, value);
            if (valueToSet == null) {
                return;
            }

            groupObject.change(form.session, valueToSet);

            updateCurrentClass = groupObject.objects.iterator().next();

            if (logger.isInfoEnabled()) {
                logger.info(String.format("changeGroupObject: [ID: %1$d]", groupObject.getID()));
                logger.info("   keys: ");
                for (Map.Entry<ObjectInstance, DataObject> entry : valueToSet.entrySet()) {
                    logger.info(String.format("     %1$s == %2$s", entry.getKey(), entry.getValue()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void expandGroupObject(int groupId, byte[] groupValues) throws RemoteException {
        try {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);
            Map<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
            if (valueToSet != null) {
                form.expandGroupObject(group, valueToSet);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        try {
            GroupObjectInstance parentGroup = form.getGroupObjectInstance(parentGroupId);
            GroupObjectInstance childGroup = form.getGroupObjectInstance(childGroupId);
            //todo:
//            form.moveGroupObject(parentGroup, deserializeGroupObjectKeys(parentGroup, parentKey));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void changeGroupObject(int groupID, byte changeType) {
        try {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
            form.changeGroupObject(groupObject, Scroll.deserialize(changeType));

            updateCurrentClass = groupObject.objects.iterator().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ClientAction> actions = new ArrayList<ClientAction>();
    private ObjectInstance updateCurrentClass = null;

    public void changePropertyDraw(int propertyID, byte[] columnKey, byte[] object, boolean all, boolean aggValue) {
        try {
            PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);
            actions.addAll(form.changeProperty(propertyDraw, keys, deserializeObject(object), this, all, aggValue));

            if (logger.isInfoEnabled()) {
                logger.info(String.format("changePropertyDraw: [ID: %1$d, SID: %2$s, all?: %3$s] = %4$s", propertyDraw.getID(), propertyDraw.getsID(), all, deserializeObject(object)));
                if (keys.size() > 0) {
                    logger.info("   columnKeys: ");
                    for (Map.Entry<ObjectInstance, DataObject> entry : keys.entrySet()) {
                        logger.info(String.format("     %1$s == %2$s", entry.getKey(), entry.getValue()));
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("   current object's values: ");
                    for (ObjectInstance obj : form.getObjects()) {
                        logger.debug(String.format("     %1$s == %2$s", obj, obj.getObjectValue()));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void groupChangePropertyDraw(int mainID, byte[] mainColumnKey, int getterID, byte[] getterColumnKey) {
        try {
            PropertyDrawInstance mainProperty = form.getPropertyDraw(mainID);
            PropertyDrawInstance getterProperty = form.getPropertyDraw(getterID);

            Map<ObjectInstance, DataObject> mainKeys = deserializePropertyKeys(mainProperty, mainColumnKey);
            Map<ObjectInstance, DataObject> getterKeys = deserializePropertyKeys(getterProperty, getterColumnKey);
            actions.addAll(
                    form.changeProperty(mainProperty, mainKeys, getterProperty, getterKeys, this, true, false)
            );

            if (logger.isInfoEnabled()) {
                logger.info(String.format("groupChangePropertyDraw: [mainID: %1$d, mainSID: %2$s, getterID: %3$d, getterSID: %4$s]",
                                          mainProperty.getID(), mainProperty.getsID(),
                                          getterProperty.getID(), getterProperty.getsID()));
                if (mainKeys.size() > 0) {
                    logger.info("   mainColumnKeys: ");
                    for (Map.Entry<ObjectInstance, DataObject> entry : mainKeys.entrySet()) {
                        logger.info(String.format("     %1$s == %2$s", entry.getKey(), entry.getValue()));
                    }
                }

                if (getterKeys.size() > 0) {
                    logger.info("   getterColumnKeys: ");
                    for (Map.Entry<ObjectInstance, DataObject> entry : getterKeys.entrySet()) {
                        logger.info(String.format("     %1$s == %2$s", entry.getKey(), entry.getValue()));
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("   current object's values: ");
                    for (ObjectInstance obj : form.getObjects()) {
                        logger.debug(String.format("     %1$s == %2$s", obj, obj.getObjectValue()));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pasteExternalTable(List propertyIDs, List<List<Object>> table) {
        try {
            form.pasteExternalTable(propertyIDs, table);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean[] getCompatibleProperties(int mainID, int[] propertiesIDs) throws RemoteException {
        Property mainProperty = form.getPropertyDraw(mainID).getChangeInstance(form.BL).property;

        int n = propertiesIDs.length;
        boolean result[] = new boolean[n];
        for (int i = 0; i < n; ++i) {
            Property property = form.getPropertyDraw(propertiesIDs[i]).getChangeInstance(form.BL).property;
            result[i] = mainProperty.getType().isCompatible( property.getType() );
        }
        return result;
    }

    @Override
    public Object getPropertyChangeValue(int propertyID) throws RemoteException {
        try {
            return form.getPropertyDraw(propertyID).getChangeInstance(form.BL).read(form.session.sql, form, form.session.env);
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        return form.canChangeClass((CustomObjectInstance) form.getObjectInstance(objectID));
    }

    public void changeGridClass(int objectID, int idClass) {
        ((CustomObjectInstance) form.getObjectInstance(objectID)).changeGridClass(idClass);
    }

    public void switchClassView(int groupID) {
        form.switchClassView(form.getGroupObjectInstance(groupID));
    }

    public void changeClassView(int groupID, ClassViewType classView) {
        form.changeClassView(form.getGroupObjectInstance(groupID), classView);
    }

    public void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
        try {
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);
            propertyDraw.toDraw.changeOrder(propertyDraw.propertyObject.getRemappedPropertyObject(keys), Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearUserFilters() {

        for (GroupObjectInstance group : form.groups) {
            group.clearUserFilters();
        }
    }

    public int countRecords(int groupObjectID) {
        try {
            return form.countRecords(groupObjectID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object calculateSum(int propertyID, byte[] columnKeys) {
        try {
            PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);
            return form.calculateSum(propertyDraw, keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                                     Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) {
        try {
            List<Map<Integer, List<byte[]>>> inMaps = new ArrayList<Map<Integer, List<byte[]>>>(BaseUtils.toList(groupMap, sumMap, maxMap));
            List<Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>>> outMaps = new ArrayList<Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>>>();
            for (Map<Integer, List<byte[]>> one : inMaps) {
                Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> outMap = new OrderedMap<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>>();
                for (Integer id : one.keySet()) {
                    PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(id);
                    List<Map<ObjectInstance, DataObject>> list = new ArrayList<Map<ObjectInstance, DataObject>>();
                    if (propertyDraw != null) {
                        for (byte[] columnKeys : one.get(id)) {
                            list.add(deserializePropertyKeys(propertyDraw, columnKeys));
                        }
                    }
                    outMap.put(propertyDraw, list);
                }
                outMaps.add(outMap);
            }
            return form.groupData(outMaps.get(0), outMaps.get(1), outMaps.get(2), onlyNotNull);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addFilter(byte[] state) {
        try {
            FilterInstance filter = FilterInstance.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
            filter.getApplyObject().addUserFilter(filter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRegularFilter(int groupID, int filterID) {
        RegularFilterGroupInstance filterGroup = form.getRegularFilterGroup(groupID);
        form.setRegularFilter(filterGroup, filterGroup.getFilter(filterID));
    }

    public int getID() {
        return form.entity.getID();
    }

    public String getSID() {
        return form.entity.getSID();
    }

    public void refreshData() {
        try {
            form.refreshData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasClientApply() {
        return form.entity.hasClientApply();
    }

    public ClientApply checkClientChanges() throws RemoteException {
        try {
            String result = form.checkApply();
            if (result != null) {
                return new CheckFailed(result);
            } else {
                return form.entity.getClientApply(form);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyClientChanges(Object clientResult) throws RemoteException {
        try {
            form.commitApply(form.entity.checkClientApply(clientResult), actions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyChanges() throws RemoteException {
        applyChanges(actions);
    }

    public void applyChanges(List<ClientAction> actions) {
        try {
            actions.addAll(form.fireOnApply(this));
            form.applyActionChanges(actions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void dialogClosed() throws RemoteException {
        try {
            actions.addAll(form.fireOnOk(this));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void continueAutoActions() throws RemoteException {
        try {
            actions.addAll(form.continueAutoActions());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelChanges() {
        try {
            form.cancelChanges();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBaseClassByteArray(int objectID) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getObjectInstance(objectID).getBaseClass().serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {

        List<CustomClass> childClasses = (((CustomObjectInstance) form.getObjectInstance(objectID)).baseClass).findClassID(classID).children;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        try {
            dataStream.writeInt(childClasses.size());
            for (CustomClass cls : childClasses) {
                cls.serialize(dataStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getPropertyChangeType(int propertyID, byte[] columnKey, boolean aggValue) {

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);
            form.serializePropertyEditorType(dataStream, propertyDraw, keys, aggValue);

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createClassPropertyDialog(viewID, value);
            return new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createObjectEditorDialog(viewID);
            return dialogForm == null
                    ? null
                    : new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createEditorPropertyDialog(viewID);
            return new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = form.createForm(formEntity, mapObjects, false, false);
            if(!formInstance.areObjectsFounded())
                return null;
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteForm createForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, formInstance.entity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
