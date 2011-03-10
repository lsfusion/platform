package platform.server.form.instance.remote;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectHierarchy;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.listener.CurrentClassListener;
import platform.server.form.view.FormView;
import platform.server.form.view.report.ReportDesignGenerator;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.serialization.SerializationType;
import platform.server.serialization.ServerContext;
import platform.server.serialization.ServerSerializationPool;

import java.io.*;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.deserializeObject;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>, F extends FormInstance<T>> extends platform.interop.remote.RemoteObject implements RemoteFormInterface {
    private final static Logger logger = Logger.getLogger(RemoteForm.class);

    public final F form;
    public final FormView richDesign;

    private final WeakReference<CurrentClassListener> weakCurrentClassListener;

    private CurrentClassListener getCurrentClassListener() {
        return weakCurrentClassListener.get();
    }

    public RemoteForm(F form, FormView richDesign, int port, CurrentClassListener currentClassListener) throws RemoteException {
        super(port);

        this.form = form;
        this.richDesign = richDesign;
        this.weakCurrentClassListener = new WeakReference<CurrentClassListener>(currentClassListener);
    }

    public byte[] getReportHierarchyByteArray() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            Map<String, List<String>> dependencies = form.entity.getReportHierarchy().getReportHierarchyMap();
            objOut.writeUTF(GroupObjectHierarchy.rootNodeName);
            objOut.writeObject(dependencies);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getReportDesignsByteArray(boolean toExcel) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            Map<String, JasperDesign> res = getReportDesigns(toExcel);
            objOut.writeObject(res);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getReportSourcesByteArray() {
        ReportSourceGenerator<T> sourceGenerator = new ReportSourceGenerator<T>(form, form.entity.getReportHierarchy());
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

    private Map<String, JasperDesign> getCustomReportDesigns() {
        try {
            GroupObjectHierarchy.ReportHierarchy hierarchy = form.entity.getReportHierarchy();
            Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();
            List<String> ids = new ArrayList<String>();
            ids.add(GroupObjectHierarchy.rootNodeName);
            for (GroupObjectHierarchy.ReportNode node : hierarchy.getAllNodes()) {
                ids.add(node.getID());
            }
            for (String id : ids) {
                File subReportFile = new File(getCustomReportName(id));
                if (subReportFile.exists()) {
                    JasperDesign subreport = JRXmlLoader.load(subReportFile);
                    designs.put(id, subreport);
                }
            }
            return designs;
        } catch (JRException e) {
            return null;
        }
    }

    private Map<String, JasperDesign> getReportDesigns(boolean toExcel) {
        if (hasCustomReportDesign()) {
            Map<String, JasperDesign> designs = getCustomReportDesigns();
            if (designs != null) {
                return designs;
            }
        }

        Set<Integer> hidedGroupsId = new HashSet<Integer>();
        for (GroupObjectInstance group : form.groups) {
            if (group.curClassView == ClassViewType.HIDE) {
                hidedGroupsId.add(group.getID());
            }
        }
        try {
            ReportDesignGenerator generator =
                    new ReportDesignGenerator(richDesign, form.entity.getReportHierarchy(), hidedGroupsId, toExcel);
            Map<String, JasperDesign> designs = generator.generate();
            for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
                String id = entry.getKey();
                String reportName = getAutoReportName(id);

                new File(reportName).getParentFile().mkdirs();

                JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            }
            return designs;
        } catch (JRException e) {
            throw new RuntimeException("Ошибка при создании дизайна", e);
        }
    }

    private String getCustomReportName(String name) {
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            return "reports/custom/" + getSID() + ".jrxml";
        } else {
            return "reports/custom/" + getSID() + "_" + name + ".jrxml";
        }
    }

    private String getAutoReportName(String name) {
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            return "reports/auto/" + getSID() + ".jrxml";
        } else {
            return "reports/auto/" + getSID() + "_" + name + ".jrxml";
        }
    }

    public boolean hasCustomReportDesign() {
        return new File(getCustomReportName(GroupObjectHierarchy.rootNodeName)).exists();
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
            CurrentClassListener currentClassListener = getCurrentClassListener();
            if (currentClass != null && currentClassListener != null && currentClassListener.changeCurrentClass(currentClass)) {
                objectClassID = currentClass.ID;
            }

            updateCurrentClass = null;
        }
        return new RemoteChanges(formChanges, remoteActions, objectClassID);
    }

    private Map<ObjectInstance, DataObject> deserializeKeys(GroupObjectInstance group, byte[] treePathKeys) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(treePathKeys));

        Map<ObjectInstance, Object> mapValues = new HashMap<ObjectInstance, Object>();
        for (GroupObjectInstance treeGroup : group.getUpTreeGroups()) {
            for (ObjectInstance objectInstance : treeGroup.objects) {
                mapValues.put(objectInstance, deserializeObject(inStream));
            }
        }

        return group.findGroupObjectValue(mapValues);
    }

    public void changeGroupObject(int groupID, byte[] value) {

        try {
            GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
            groupObject.change(form.session, deserializeKeys(groupObject, value));

            updateCurrentClass = groupObject.objects.iterator().next();

            if (logger.isInfoEnabled()) {
                logger.info(String.format("changeGroupObject: [ID: %1$d]", groupObject.getID()));
                logger.info("   keys: ");
                for (Map.Entry<ObjectInstance, DataObject> entry : deserializeKeys(groupObject, value).entrySet()) {
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
            form.expandGroupObject(group, deserializeKeys(group, groupValues));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        try {
            GroupObjectInstance parentGroup = form.getGroupObjectInstance(parentGroupId);
            GroupObjectInstance childGroup = form.getGroupObjectInstance(childGroupId);
            //todo:
//            form.moveGroupObject(parentGroup, deserializeKeys(parentGroup, parentKey));
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

    public void changePropertyDraw(int propertyID, byte[] object, boolean all, byte[] columnKeys) {
        try {
            PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);
            Map<ObjectInstance, DataObject> keys = deserializeKeys(propertyDraw, columnKeys);
            actions.addAll(form.changeProperty(propertyDraw, deserializeObject(object), this, all, keys));

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

    private Map<ObjectInstance, DataObject> deserializeKeys(PropertyDrawInstance<?> propertyDraw, byte[] columnKeys) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(columnKeys));
        Map<ObjectInstance, DataObject> keys = new HashMap<ObjectInstance, DataObject>();
        for (GroupObjectInstance groupInstance : propertyDraw.columnGroupObjects) {
            Map<ObjectInstance, Object> mapValues = new HashMap<ObjectInstance, Object>();
            boolean found = false;
            for (ObjectInstance objectInstance : groupInstance.objects) {
                Object val = deserializeObject(inStream);
                if (val != null) {
                    mapValues.put(objectInstance, val);
                    found = true;
                }
            }

            if (found) {
                keys.putAll(groupInstance.findGroupObjectValue(mapValues));
            }
        }
        return keys;
    }

    public void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        PropertyDrawInstance<?> propertyDraw = form.getPropertyDraw(propertyID);
        try {
            Map<ObjectInstance, DataObject> keys = deserializeKeys(propertyDraw, columnKeys);
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

    public Object calculateSum(int groupObjectID, int propertyID) {
        try {
            return form.calculateSum(groupObjectID, propertyID);
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
        try {
            actions.addAll(form.fireOnApply(this));
            form.applyActionChanges(actions);
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

    public byte[] getPropertyChangeType(int propertyID) {

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            form.serializePropertyEditorType(dataStream, form.getPropertyDraw(propertyID));

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createClassPropertyDialog(viewID, value);
            return new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getCurrentClassListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createObjectEditorDialog(viewID);
            return dialogForm == null
                    ? null
                    : new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getCurrentClassListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createEditorPropertyDialog(viewID);
            return new RemoteDialog<T>(dialogForm, dialogForm.entity.getRichDesign(), exportPort, getCurrentClassListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = form.createForm(formEntity, mapObjects);
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), exportPort, getCurrentClassListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteForm createForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, formInstance.entity.getRichDesign(), exportPort, getCurrentClassListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
