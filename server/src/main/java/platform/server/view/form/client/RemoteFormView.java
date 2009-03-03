package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.RemoteFormImplement;
import platform.server.view.form.*;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.session.ChangeValue;

import java.sql.SQLException;
import java.io.*;
import java.util.List;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

// фасад для работы с клиентом
public class RemoteFormView extends RemoteFormImplement implements RemoteFormInterface {

    RemoteForm form;
    public FormView richDesign;
    public JasperDesign reportDesign;

    public RemoteFormView(RemoteForm iForm, FormView iRichDesign, JasperDesign iReportDesign) throws RemoteException {
        super();
        form = iForm;
        richDesign = iRichDesign;
        reportDesign = iReportDesign;
    }

    public byte[] getReportDesignByteArray() throws RemoteException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(outStream).writeObject(reportDesign);
        } catch (IOException e) {
            throw new RemoteException("IO exception : "+e.getMessage());
        }
        return outStream.toByteArray();
    }

    public byte[] getReportDataByteArray() throws RemoteException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getReportData().serialize(new DataOutputStream(outStream));
        } catch (Exception e) {
            throw new RemoteException("Exception : " + e.getMessage());
        }
        return outStream.toByteArray();
    }

    public int getGID() {
        return form.getGID();
    }

    public byte[] getRichDesignByteArray() {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            richDesign.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    public byte[] getFormChangesByteArray() throws RemoteException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.endApply().serialize(new DataOutputStream(outStream));
        } catch (Exception e) {
            throw new RemoteException("Exception : " + e.getMessage());
        }
        return outStream.toByteArray();
    }

    public void changeGroupObject(int groupID, byte[] value) throws RemoteException {
        
        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        try {
            form.changeGroupObject(groupObject, new GroupObjectValue(new DataInputStream(new ByteArrayInputStream(value)), groupObject));
        } catch (Exception e) {
            throw new RemoteException("Exception : " + e.getMessage());
        }
    }

    public int getObjectClassID(int objectID) {
        return form.getObjectImplement(objectID).objectClass.ID;
    }

    public void changeGroupObject(int groupID, int changeType) throws RemoteException {
        try {
            form.changeGroupObject(form.getGroupObjectImplement(groupID), changeType);
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void changePropertyView(int propertyID, byte[] object, boolean externalID) throws RemoteException {
        try {
            form.changePropertyView(form.getPropertyView(propertyID), BaseUtils.deserializeObject(object), externalID);
        } catch (Exception e) {
            throw new RemoteException("Exception : " + e.getMessage());
        }
    }

    public void changeObject(int objectID, Integer value) throws RemoteException {
        try {
            form.changeObject(form.getObjectImplement(objectID), value);
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void addObject(int objectID, int classID) throws RemoteException {
        ObjectImplement object = form.getObjectImplement(objectID);
        try {
            form.addObject(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void changeClass(int objectID, int classID) throws RemoteException {
        ObjectImplement object = form.getObjectImplement(objectID);
        try {
            form.changeClass(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void changeGridClass(int objectID,int idClass) throws RemoteException {
        try {
            form.changeGridClass(form.getObjectImplement(objectID), idClass);
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void switchClassView(int groupID) {
        form.switchClassView(form.getGroupObjectImplement(groupID));
    }

    public void changeOrder(int propertyID, int modiType) {
        form.changeOrder(form.getPropertyView(propertyID), modiType);
    }

    public void clearUserFilters() {
        form.clearUserFilters();
    }

    public void addFilter(byte[] state) {
        try {
            form.addUserFilter(new Filter(new DataInputStream(new ByteArrayInputStream(state)), form));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRegularFilter(int groupID, int filterID) {
        RegularFilterGroup filterGroup = form.getRegularFilterGroup(groupID);
        form.setRegularFilter(filterGroup, filterGroup.getFilter(filterID));
    }

    public int getID() {
        return form.getID();
    }

    public void refreshData() {
        form.refreshData();
    }

    public boolean hasSessionChanges() {
        return form.hasSessionChanges();
    }

    public String saveChanges() throws RemoteException {
        try {
            return form.saveChanges();
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public void cancelChanges() throws RemoteException {
        try {
            form.cancelChanges();
        } catch (SQLException e) {
            throw new RemoteException("SQL Exception : " + e.getMessage());
        }
    }

    public byte[] getBaseClassByteArray(int objectID) throws RemoteException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getObjectImplement(objectID).baseClass.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RemoteException("IO Exception : " + e.getMessage());
        }
        return outStream.toByteArray();
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {

        List<RemoteClass> childClasses = form.getObjectImplement(objectID).baseClass.findClassID(classID).childs;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        try {
            dataStream.writeInt(childClasses.size());
            for (RemoteClass cls : childClasses)
                cls.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) throws RemoteException {

        try {
            ChangeValue changeValue = form.getPropertyEditorObjectValue(form.getPropertyView(propertyID), externalID);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            dataStream.writeBoolean(changeValue==null);
            if(changeValue!=null)
                changeValue.serialize(dataStream);

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RemoteException("Exception : " + e.getMessage());
        }
    }
}
