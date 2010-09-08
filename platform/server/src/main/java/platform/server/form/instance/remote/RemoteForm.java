package platform.server.form.instance.remote;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.CompressingOutputStream;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientApply;
import platform.interop.action.ResultClientAction;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.listener.CurrentClassListener;
import platform.server.form.view.FormView;
import platform.server.form.view.report.DefaultJasperDesign;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

// фасад для работы с клиентом
public class RemoteForm<T extends BusinessLogics<T>,F extends FormInstance<T>> extends platform.interop.remote.RemoteObject implements RemoteFormInterface {

    public final F form;
    public final FormView richDesign;
    public final JasperDesign reportDesign;

    private final CurrentClassListener currentClassListener;

    public RemoteForm(F form, FormView richDesign, JasperDesign reportDesign, int port, CurrentClassListener currentClassListener) throws RemoteException {
        super(port);
        
        this.form = form;
        this.richDesign = richDesign;
        this.reportDesign = reportDesign;
        this.currentClassListener = currentClassListener;
    }

    private JasperDesign getReportDesign(boolean toExcel) {

        if (reportDesign != null) return reportDesign;

        Set<Integer> hideGroupObjects = new HashSet<Integer>();
        for (GroupObjectInstance group : form.groups)
            if (group.curClassView == ClassViewType.HIDE)
                hideGroupObjects.add(group.getID());

        try {

            File customReport = new File(getCustomReportName());
            if (customReport.exists()) {
                return JRXmlLoader.load(customReport);
            }

            JasperDesign design = new DefaultJasperDesign(richDesign, hideGroupObjects, toExcel).design;

            String autoReportName = getAutoReportName();
            if (!(new File(autoReportName).exists())) {
                DynamicJasperHelper.generateJRXML(JasperCompileManager.compileReport(design),
                                                  "UTF-8", autoReportName);
            }

            return design;
            
        } catch (JRException e) {
            throw new RuntimeException("Ошибка при создании дизайна отчета по умолчанию", e);
        }
    }

    private String getCustomReportName() {
        return "reports/custom/" + getSID() + ".jrxml";
    }

    private String getAutoReportName() {
        return "reports/auto/" + getSID() + ".jrxml";
    }

    public boolean hasCustomReportDesign() {
        return reportDesign != null || new File(getCustomReportName()).exists();
    }

    public byte[] getReportDesignByteArray(boolean toExcel) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            CompressingOutputStream compStream = new CompressingOutputStream(outStream);
            ObjectOutputStream objOut = new ObjectOutputStream(compStream);
            objOut.writeUTF(richDesign.caption);
            objOut.writeObject(getReportDesign(toExcel));
            compStream.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getReportDataByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            CompressingOutputStream compStream = new CompressingOutputStream(outStream);
            form.getFormData(hasCustomReportDesign()).serialize(new DataOutputStream(compStream));
            compStream.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getRichDesignByteArray() {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            CompressingOutputStream compStream = new CompressingOutputStream(outStream);
            richDesign.serialize(new DataOutputStream(compStream)); 
            compStream.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public void changePageSize(int groupID, int pageSize) throws RemoteException {

        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
        form.changePageSize(groupObject, pageSize);
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    private byte[] getFormChangesByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            CompressingOutputStream compStream = new CompressingOutputStream(outStream);
            form.endApply().serialize(new DataOutputStream(compStream));
            compStream.finish();
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
        if(updateCurrentClass!=null) {
            ConcreteCustomClass currentClass = form.getObjectClass(updateCurrentClass);
            if(currentClass != null && currentClassListener.changeCurrentClass(currentClass))
                objectClassID = currentClass.ID;

            updateCurrentClass = null;
        }
        return new RemoteChanges(formChanges, remoteActions, objectClassID);
    }

    public void changeGroupObject(int groupID, byte[] value) {
        
        GroupObjectInstance groupObject = form.getGroupObjectInstance(groupID);
        try {

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(value));
            // считаем ключи и найдем groupObjectValue
            Map<ObjectInstance,Object> mapValues = new HashMap<ObjectInstance, Object>();
            for(ObjectInstance object : groupObject.objects)
                mapValues.put(object, BaseUtils.deserializeObject(inStream));
            form.changeGroupObject(groupObject, groupObject.findGroupObjectValue(mapValues));

            updateCurrentClass = groupObject.objects.iterator().next();
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

    public void changePropertyDraw(int propertyID, byte[] object, boolean all) {
        try {
            actions.addAll(form.changeProperty(form.getPropertyDraw(propertyID), BaseUtils.deserializeObject(object), this, all));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void changePropertyDrawWithColumnKeys(int propertyID, byte[] object, boolean all, byte[] columnKeys) {
        try {
            PropertyDrawInstance propertyDraw = form.getPropertyDraw(propertyID);

            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(columnKeys));
            Map<ObjectInstance, DataObject> mapDataValues = new HashMap<ObjectInstance, DataObject>();
            for (GroupObjectInstance groupInstance : propertyDraw.columnGroupObjects) {
                Map<ObjectInstance, Object> mapValues = new HashMap<ObjectInstance, Object>();
                for (ObjectInstance objectInstance : groupInstance.objects) {
                    mapValues.put(objectInstance, BaseUtils.deserializeObject(inStream));
                }
                mapDataValues.putAll( groupInstance.findGroupObjectValue(mapValues) );
            }

            actions.addAll(form.changeProperty(propertyDraw, BaseUtils.deserializeObject(object), this, all, mapDataValues));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void changeObject(int objectID, Object value) {
        try {
            ObjectInstance objectImplement = form.getObjectInstance(objectID);
            actions.addAll(form.changeObject(objectImplement, value, this));
            
            updateCurrentClass = objectImplement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addObject(int objectID, int classID) {
        CustomObjectInstance object = (CustomObjectInstance) form.getObjectInstance(objectID);
        try {
            form.addObject(object, (classID == -1) ? null : object.baseClass.findConcreteClassID(classID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeClass(int objectID, int classID) {
        try {
            CustomObjectInstance objectImplement = (CustomObjectInstance) form.getObjectInstance(objectID);
            form.changeClass(objectImplement, classID);

            updateCurrentClass = objectImplement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        return form.canChangeClass((CustomObjectInstance)form.getObjectInstance(objectID));
    }

    public void changeGridClass(int objectID,int idClass) {
        ((CustomObjectInstance) form.getObjectInstance(objectID)).changeGridClass(idClass);
    }

    public void switchClassView(int groupID) {
        form.switchClassView(form.getGroupObjectInstance(groupID));
    }

    public void changeClassView(int groupID, byte classView) {
        form.changeClassView(form.getGroupObjectInstance(groupID), classView);
    }

    public void changePropertyOrder(int propertyID, byte modiType) {
        PropertyDrawInstance<?> propertyView = form.getPropertyDraw(propertyID);
        try {
            propertyView.toDraw.changeOrder(propertyView.propertyObject, Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeObjectOrder(int objectID, byte modiType) {
        ObjectInstance object = form.getObjectInstance(objectID);
        try {
            object.groupTo.changeOrder(object, Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeObjectClassOrder(int propertyID, byte modiType) throws RemoteException {
        CustomObjectInstance object = (CustomObjectInstance)form.getObjectInstance(propertyID);
        try {
            object.groupTo.changeOrder(object.objectClassInstance, Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearUserFilters() {

        for(GroupObjectInstance group : form.groups)
            group.clearUserFilters();
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
        return form.entity.ID;
    }

    public String getSID() {
        return form.entity.sID;
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

    public ClientApply getClientApply() throws RemoteException {
        try {
            String checkString = form.checkChanges();
            if(checkString!=null)
                return new CheckFailed(checkString);            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return form.entity.getClientApply(form);
    }

    public void applyClientChanges(Object clientResult) {
        String checkClientApply = form.entity.checkClientApply(clientResult);
        try {
            if(checkClientApply!=null) {
                form.rollbackChanges();
                actions.add(new ResultClientAction(checkClientApply, true));
            } else {
                form.writeChanges();
                actions.add(new ResultClientAction("Изменения были удачно записаны...", false));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void applyChanges() throws RemoteException {
        try {
            String result = form.applyChanges();
            if(result!=null)
                actions.add(new ResultClientAction(result, true));
            else
                actions.add(new ResultClientAction("Изменения были удачно записаны...", false));
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

        List<CustomClass> childClasses = (((CustomObjectInstance)form.getObjectInstance(objectID)).baseClass).findClassID(classID).children;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        try {
            dataStream.writeInt(childClasses.size());
            for (CustomClass cls : childClasses)
                cls.serialize(dataStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getPropertyChangeType(int propertyID) {

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            form.serializePropertyEditorType(dataStream,form.getPropertyDraw(propertyID));

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createClassPropertyDialog(viewID, value);
            return new RemoteDialog<T>(dialogForm,dialogForm.entity.getRichDesign(),dialogForm.entity.getReportDesign(),exportPort, currentClassListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createEditorPropertyDialog(viewID);
            return new RemoteDialog<T>(dialogForm,dialogForm.entity.getRichDesign(),dialogForm.entity.getReportDesign(),exportPort, currentClassListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createObjectDialog(int objectID) {
        try {
            DialogInstance<T> dialogForm = form.createObjectDialog(objectID);
            return new RemoteDialog<T>(dialogForm,dialogForm.entity.getRichDesign(),dialogForm.entity.getReportDesign(),exportPort, currentClassListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteDialogInterface createObjectDialogWithValue(int objectID, int value) throws RemoteException {
        try {
            DialogInstance<T> dialogForm = form.createObjectDialog(objectID, value);
            return new RemoteDialog<T>(dialogForm,dialogForm.entity.getRichDesign(),dialogForm.entity.getReportDesign(),exportPort, currentClassListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteFormInterface createForm(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects) {
        try {
            FormInstance<T> formInstance = form.createForm(formEntity, mapObjects);
            return new RemoteForm<T, FormInstance<T>>(formInstance, formEntity.getRichDesign(), formEntity.getReportDesign(),exportPort, currentClassListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
