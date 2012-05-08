package platform.server.form.instance.remote;

import com.google.common.base.Throwables;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.form.*;
import platform.server.ContextAwareDaemonThreadFactory;
import platform.server.RemoteContextObject;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.type.Type;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.FilterInstance;
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
import platform.server.session.DataSession;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static platform.base.BaseUtils.deserializeObject;
import static platform.base.BaseUtils.serializeObject;
import static platform.server.data.type.TypeSerializer.serializeType;
import static platform.server.form.entity.GroupObjectHierarchy.ReportNode;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>, F extends FormInstance<T>> extends RemoteContextObject implements RemoteFormInterface {
    private final static Logger logger = Logger.getLogger(RemoteForm.class);

    public final F form;
    public final FormView richDesign;

    private final WeakReference<RemoteFormListener> weakRemoteFormListener;

    private final boolean checkOnOk;

    public RemoteForm(F form, FormView richDesign, int port, RemoteFormListener remoteFormListener) throws RemoteException {
        this(form, richDesign, port, remoteFormListener, false);
    }

    public RemoteForm(F form, FormView richDesign, int port, RemoteFormListener remoteFormListener, boolean checkOnOk) throws RemoteException {
        super(port);

        this.form = form;
        this.richDesign = richDesign;
        this.checkOnOk = checkOnOk;
        this.weakRemoteFormListener = new WeakReference<RemoteFormListener>(remoteFormListener);
        if (remoteFormListener != null) {
            remoteFormListener.formCreated(this);
        }
    }

    public RemoteFormListener getRemoteFormListener() {
        return weakRemoteFormListener.get();
    }

    private void emitExceptionIfHasActiveInvocation() {
        if (currentInvocation != null && currentInvocation.isPaused()) {
            //стопаем старый рабочий поток, чтобы можно было запустить новый без ожидания окончания старого
            currentInvocation.cancel();
            currentInvocation = null;
            throw new RuntimeException("There is already invocation executing...");
        }
    }

    public byte[] getReportHierarchyByteArray() {
        emitExceptionIfHasActiveInvocation();

        Map<String, List<String>> dependencies = form.entity.getReportHierarchy().getReportHierarchyMap();
        return getReportHierarchyByteArray(dependencies);
    }

    public byte[] getSingleGroupReportHierarchyByteArray(int groupId) {
        emitExceptionIfHasActiveInvocation();

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

    public byte[] getReportDesignsByteArray(boolean toExcel, FormUserPreferences userPreferences) {
        emitExceptionIfHasActiveInvocation();

        return getReportDesignsByteArray(toExcel, null, userPreferences);
    }

    /// Отчет по одной группе
    public byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId, FormUserPreferences userPreferences) {
        emitExceptionIfHasActiveInvocation();

        return getReportDesignsByteArray(toExcel, groupId, userPreferences);
    }

    private byte[] getReportDesignsByteArray(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            Map<String, JasperDesign> res = getReportDesigns(toExcel, groupId, userPreferences);
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
        emitExceptionIfHasActiveInvocation();

        GroupObjectHierarchy.ReportHierarchy hierarchy = form.entity.getReportHierarchy();
        ReportSourceGenerator<T> sourceGenerator = new ReportSourceGenerator<T>(form, hierarchy, getGridGroups(null));
        return getReportSourcesByteArray(sourceGenerator);
    }

    public byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException {
        emitExceptionIfHasActiveInvocation();

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
                        serializeObject(dataStream, obj);
                    }
                    serializeObject(dataStream, valueEntry.getValue());
                }
            }

            serializePropertyObjects(dataStream, columnGroupCaptions.columnObjects);

            for (Map.Entry<String, LinkedHashSet<List<Object>>> entry : columnGroupCaptions.columnData.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                LinkedHashSet<List<Object>> value = entry.getValue();
                dataStream.writeInt(value.size());
                for (List<Object> list : value) {
                    for (Object obj : list) {
                        serializeObject(dataStream, obj);
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

    private InputStream getCustomReportInputStream(String sid, ReportNode node, boolean toExcel, Integer groupId) throws SQLException {
        InputStream iStream = null;
        if (node != null) {
            PropertyObjectEntity reportPathProp = node.getGroupList().get(0).reportPathProp;
            if (reportPathProp != null) {
                PropertyObjectInstance propInstance = form.instanceFactory.getInstance(reportPathProp);
                String reportPath = (String) propInstance.read(form.session, form);
                if (reportPath != null) {
                    String resourceName = "/" + getVariableCustomReportName(getReportPrefix(toExcel, groupId) + reportPath.trim());
                    iStream = getClass().getResourceAsStream(resourceName);
                }
            }
        }
        if (iStream == null) {
            String resourceName = "/" + getCustomReportName(sid, getDefaultReportSID(toExcel, groupId));
            iStream = getClass().getResourceAsStream(resourceName);
        }
        return iStream;
    }

    private Map<String, JasperDesign> getCustomReportDesigns(boolean toExcel, Integer groupId) {
        try {
            GroupObjectHierarchy.ReportHierarchy hierarchy = getReportHierarchy(groupId);
            Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();
            List<Pair<String, ReportNode>> nodes = new ArrayList<Pair<String, ReportNode>>();
            nodes.add(new Pair<String, ReportNode>(GroupObjectHierarchy.rootNodeName, null));
            for (GroupObjectHierarchy.ReportNode node : hierarchy.getAllNodes()) {
                nodes.add(new Pair<String, ReportNode>(node.getID(), node));
            }
            for (Pair<String, ReportNode> node : nodes) {
                InputStream iStream = getCustomReportInputStream(node.first, node.second, toExcel, groupId);
                // Если не нашли custom design для xls, пробуем найти обычный
                if (toExcel && iStream == null) {
                    iStream = getCustomReportInputStream(node.first, node.second, false, groupId);
                }
                if (iStream == null) {
                    return null;
                }
                JasperDesign subreport = JRXmlLoader.load(iStream);
                designs.put(node.first, subreport);
            }
            return designs;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, String> getReportPath(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) {
        emitExceptionIfHasActiveInvocation();

        Map<String, String> ret = new HashMap<String, String>();

        String sid = getDefaultReportSID(toExcel, groupId);
        Map<String, JasperDesign> customDesigns = getCustomReportDesigns(toExcel, groupId);
        if (customDesigns != null) {
            Set<String> keySet = customDesigns.keySet();
            for (String key : keySet){
                ret.put(
                        System.getProperty("user.dir") + "/src/main/resources/" + getCustomReportName(key, sid),
                        System.getProperty("user.dir") + "/target/classes/" + getCustomReportName(key, sid)
                );
            }
        } else {
            Set<Integer> hidedGroupsId = new HashSet<Integer>();
            for (GroupObjectInstance group : form.groups) {
                if (group.curClassView == ClassViewType.HIDE || groupId != null && groupId != group.getID()) {
                    hidedGroupsId.add(group.getID());
                }
            }
            try {
                ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId), hidedGroupsId, userPreferences, toExcel);
                Map<String, JasperDesign> designs = generator.generate();
                String reportName;
                for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
                    String id = entry.getKey();
                    reportName = getAutoReportName(id, sid);
                    new File(reportName).getParentFile().mkdirs();
                    JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
                    ret.put(
                            System.getProperty("user.dir") + "/" + reportName,
                            System.getProperty("user.dir") + "/target/classes/reports/custom/" + sid
                    );

                }

            } catch (JRException e) {
                throw new RuntimeException(ServerResourceBundle.getString("form.instance.error.creating.design"), e);
            }
        }
        return ret;
    }

    private static final String xlsPrefix = "xls_";
    private static final String tablePrefix = "table";

    private String getReportPrefix(boolean toExcel, Integer groupId) {
        String prefix = (toExcel ? xlsPrefix : "");
        return prefix + (groupId == null ? "" : tablePrefix + form.getGroupObjectInstance(groupId).getSID() + "_");
    }

    private String getDefaultReportSID(boolean toExcel, Integer groupId) {
        return getReportPrefix(toExcel, groupId) + getSID();
    }

    private Map<String, JasperDesign> getReportDesigns(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) {
        String sid = getDefaultReportSID(toExcel, groupId);
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
            ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId), hidedGroupsId, userPreferences, toExcel);
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

    private String getVariableCustomReportName(String name) {
        if (!name.endsWith(".jrxml")) {
            name = name + ".jrxml";
        }
        return "reports/custom/" + name;
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

    public byte[] getRichDesignByteArray() {
        emitExceptionIfHasActiveInvocation();

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
        //пока разрешаем вызов changePageSize, т.к. он может прийти при показе модального диалога...
//        emitExceptionIfHasActiveInvocation();

        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
        form.changePageSize(groupObject, pageSize);
    }

    public void gainedFocus() {
//        emitExceptionIfHasActiveInvocation();

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
            Throwables.propagate(e);
        }

        return outStream.toByteArray();
    }

    public RemoteChanges getRemoteChanges() {
        emitExceptionIfHasActiveInvocation();
        return prepareRemoteChanges();
    }

    private RemoteChanges prepareRemoteChanges() {
        byte[] formChanges = getFormChangesByteArray();

        int objectClassID = 0;
        if (updateCurrentClass != null) {
            ConcreteCustomClass currentClass = form.getObjectClass(updateCurrentClass);
            RemoteFormListener remoteFormListener = getRemoteFormListener();
            if (currentClass != null && remoteFormListener != null && remoteFormListener.currentClassChanged(currentClass)) {
                objectClassID = currentClass.ID;
            }

            updateCurrentClass = null;
        }
        RemoteChanges result = new RemoteChanges(formChanges, actions.toArray(new ClientAction[actions.size()]), objectClassID, false);
        actions.clear();
        return result;
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
        emitExceptionIfHasActiveInvocation();

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
        emitExceptionIfHasActiveInvocation();
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

    public void collapseGroupObject(int groupId, byte[] groupValues) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        try {
            GroupObjectInstance group = form.getGroupObjectInstance(groupId);
            Map<ObjectInstance, DataObject> valueToSet = deserializeGroupObjectKeys(group, groupValues);
            if (valueToSet != null) {
                form.collapseGroupObject(group, valueToSet);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
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
        emitExceptionIfHasActiveInvocation();
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

    private List<ClientAction> actions = Collections.synchronizedList(new ArrayList<ClientAction>());
    private ObjectInstance updateCurrentClass = null;

    public RemoteChanges changePropertyDraw(final int propertyID, final byte[] columnKey, final byte[] object, final boolean all, final boolean aggValue) throws RemoteException {
        assert false:"changePropertyDraw shouldn't be used anymore";
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);
                actions.addAll(form.changeProperty(propertyDraw, keys, deserializeObject(object), all, aggValue));

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
            }
        });
    }

    public RemoteChanges groupChangePropertyDraw(final int mainID, final byte[] mainColumnKey, final int getterID, final byte[] getterColumnKey) throws RemoteException {
        assert false:"groupChangePropertyDraw shouldn't be used anymore";
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                PropertyDrawInstance mainProperty = form.getPropertyDraw(mainID);
                PropertyDrawInstance getterProperty = form.getPropertyDraw(getterID);

                    Map<ObjectInstance, DataObject> mainKeys = deserializePropertyKeys(mainProperty, mainColumnKey);
                    Map<ObjectInstance, DataObject> getterKeys = deserializePropertyKeys(getterProperty, getterColumnKey);
                    actions.addAll(
                            form.changeProperty(mainProperty, mainKeys, getterProperty, getterKeys, true, false)
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
            }
        });
    }

    public RemoteChanges pasteExternalTable(final List<Integer> propertyIDs, final List<List<Object>> table) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                form.pasteExternalTable(propertyIDs, table);
            }
        });
    }

    public RemoteChanges pasteMulticellValue(final Map<Integer, List<Map<Integer, Object>>> cells, final Object value) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                form.pasteMulticellValue(cells, value);
            }
        });
    }

    public boolean[] getCompatibleProperties(int mainID, int[] propertiesIDs) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        try {
            Property mainProperty = form.getPropertyDraw(mainID).getChangeInstance(form.BL, form).property;

            int n = propertiesIDs.length;
            boolean result[] = new boolean[n];
            for (int i = 0; i < n; ++i) {
                Property property = form.getPropertyDraw(propertiesIDs[i]).getChangeInstance(form.BL, form).property;
                result[i] = mainProperty.getType().isCompatible( property.getType() );
            }
            return result;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public Object getPropertyChangeValue(int propertyID) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        try {
            return form.getPropertyDraw(propertyID).getChangeInstance(form.BL, form).read(form.session.sql, form, form.session.env);
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return form.canChangeClass((CustomObjectInstance) form.getObjectInstance(objectID));
    }

    public void changeGridClass(int objectID, int idClass) {
        emitExceptionIfHasActiveInvocation();
        ((CustomObjectInstance) form.getObjectInstance(objectID)).changeGridClass(idClass);
    }

    public void switchClassView(int groupID) {
        emitExceptionIfHasActiveInvocation();
        form.switchClassView(form.getGroupObjectInstance(groupID));
    }

    public void changeClassView(int groupID, ClassViewType classView) {
        emitExceptionIfHasActiveInvocation();
        form.changeClassView(form.getGroupObjectInstance(groupID), classView);
    }

    public void changeClassView(int groupID, String classViewName) {
        emitExceptionIfHasActiveInvocation();
        form.changeClassView(form.getGroupObjectInstance(groupID), ClassViewType.valueOf(classViewName));
    }

    public void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
        try {
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKeys);
            propertyDraw.toDraw.changeOrder(propertyDraw.propertyObject.getRemappedPropertyObject(keys), Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearUserFilters() {
        emitExceptionIfHasActiveInvocation();

        for (GroupObjectInstance group : form.groups) {
            group.clearUserFilters();
        }
    }

    public int countRecords(int groupObjectID) {
        emitExceptionIfHasActiveInvocation();
        try {
            return form.countRecords(groupObjectID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object calculateSum(int propertyID, byte[] columnKeys) {
        emitExceptionIfHasActiveInvocation();
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
        emitExceptionIfHasActiveInvocation();
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
        emitExceptionIfHasActiveInvocation();
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
        emitExceptionIfHasActiveInvocation();
        form.setRegularFilter(form.getRegularFilterGroup(groupID), filterID);
    }

    public int getID() {
        return form.entity.getID();
    }

    public String getSID() {
        emitExceptionIfHasActiveInvocation();
        return form.entity.getSID();
    }

    public void refreshData() {
        emitExceptionIfHasActiveInvocation();
        try {
            form.refreshData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasClientActionOnApply() {
        emitExceptionIfHasActiveInvocation();
        return form.entity.hasClientApply();
    }

    public ClientAction getClientActionOnApply() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        try {
            List<ClientAction> actions = new ArrayList<ClientAction>();
            if (!form.checkApply(actions)) {
                return new CheckFailed(actions);
            } else {
                return form.entity.getClientActionOnApply(form);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteChanges applyChanges() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() {
                applyChanges(actions);
            }
        });
    }

    public void applyChanges(List<ClientAction> actions) {
        try {
            form.apply(null, actions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteChanges okPressed() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                if (checkOnOk) {
                    if (!form.checkApply(actions)) {
                        return;
                    }
                }

                actions.addAll(form.fireOnOk());
            }
        });
    }

    public RemoteChanges closedPressed() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                actions.addAll(form.fireOnClose());
            }
        });
    }

    public RemoteChanges nullPressed() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return executeWithRemoteChanges(new TRunnable() {
            @Override
            public void run() throws Exception {
                actions.addAll(form.fireOnNull());
            }
        });
    }

    @Override
    public void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        form.saveUserPreferences(preferences, forAllUsers);
    }

    @Override
    public FormUserPreferences loadUserPreferences() throws RemoteException {
        emitExceptionIfHasActiveInvocation();
        return form.loadUserPreferences();
    }

    public void cancelChanges() {
        emitExceptionIfHasActiveInvocation();
        try {
            form.cancel();
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
        assert false:"getPropertyChangeType shouldn't be used anymore";
        try {
            PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
            Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);

            return form.serializePropertyChangeType(propertyDraw, keys, aggValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException {
        assert false:"createObjectEditorDialog shouldn't be used anymore";
        try {
            DialogInstance<T> dialogForm = form.createObjectEditorDialog(form.getPropertyDraw(viewID));
            return dialogForm == null ? null : createRemoteDialog(dialogForm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createChangeEditorDialog(int viewID) throws RemoteException {
        assert false:"createChangeEditorDialog shouldn't be used anymore";
        try {
            DialogInstance<T> dialogForm = form.createChangeEditorDialog(form.getPropertyDraw(viewID));
            return createRemoteDialog(dialogForm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = form.createForm(formEntity, mapObjects, false, false);
            if(!formInstance.areObjectsFound())
                return null;
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance, boolean checkOnOk) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, formInstance.entity.getRichDesign(), exportPort, getRemoteFormListener(), checkOnOk);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        try {
            return new RemoteDialog(dialogInstance, dialogInstance.entity.getRichDesign(), exportPort, getRemoteFormListener());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean newSession, boolean interactive) throws SQLException {
        return form.createForm(formEntity, mapObjects, session, newSession, interactive);
    }

    @Override
    public BusinessLogics getBL() {
        return form.BL;
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

    private final ExecutorService pausablesExecutor = Executors.newCachedThreadPool(new ContextAwareDaemonThreadFactory(this));
    private RemotePausableInvocation<?> currentInvocation = null;

    private <T> T executeInvocation(RemotePausableInvocation<T> invocation) throws RemoteException {
        emitExceptionIfHasActiveInvocation();

        currentInvocation = invocation;
        return invocation.execute();
    }

    private RemoteChanges executeWithRemoteChanges(final TRunnable runnable) throws RemoteException {
        return executeInvocation(new RemotePausableInvocation<RemoteChanges>(pausablesExecutor) {
            @Override
            protected void runInvocation() throws Throwable {
                threads.add(Thread.currentThread());
                try {
                    runnable.run();
                } finally {
                    threads.remove(Thread.currentThread());
                }
            }

            @Override
            protected RemoteChanges handleUserInteractionRequest(ClientAction... acts) {
                return new RemoteChanges(null, acts, 0, true);
            }

            @Override
            protected RemoteChanges handleFinished() throws RemoteException {
                return prepareRemoteChanges();
            }
        });
    }

    public RemoteChanges continueRemoteChanges(Object[] actionResults) throws RemoteException {
        return (RemoteChanges) currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    public EditActionResult executeEditAction(final int propertyID, final byte[] columnKey, final String actionSID) throws RemoteException {
        return executeInvocation(new RemotePausableInvocation<EditActionResult>(pausablesExecutor) {
            @Override
            protected EditActionResult callInvocation() throws Throwable {
                threads.add(Thread.currentThread());
                try {
                    PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
                    Map<ObjectInstance, DataObject> keys = deserializePropertyKeys(propertyDraw, columnKey);

                    List<ClientAction> actions = form.executeEditAction(propertyDraw, actionSID, keys);

                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("executeEditAction: [ID: %1$d, SID: %2$s]", propertyDraw.getID(), propertyDraw.getsID()));
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

                    if (actions != null) {
                        RemoteForm.this.actions.addAll(actions);
                    }

                    return EditActionResult.finished;
                } finally {
                    threads.remove(Thread.currentThread());
                }
            }

            @Override
            protected EditActionResult handleUserInteractionRequest(ClientAction... actions) {
                return new EditActionResult(actions);
            }

            @Override
            protected EditActionResult handleUserInputRequest(Type type, Object oldValue) throws Exception {
                return new EditActionResult(serializeType(type), serializeObject(oldValue));
            }
        });
    }

    public EditActionResult continueExecuteEditAction(UserInputResult inputResult) throws RemoteException {
        return (EditActionResult) currentInvocation.resumeAfterUserInput(inputResult);
    }

    public EditActionResult continueExecuteEditAction(Object[] actionResults) throws RemoteException {
        return (EditActionResult) currentInvocation.resumeAfterUserInteraction(actionResults);
    }

    public Object[] requestUserInteraction(ClientAction... acts) {
        assert currentInvocation != null;
        return currentInvocation.pauseForUserInteraction(acts);
    }

    public UserInputResult requestUserInput(Type type, Object oldValue) {
        assert currentInvocation != null;
        return currentInvocation.pauseForUserInput(type, oldValue);
    }
}
