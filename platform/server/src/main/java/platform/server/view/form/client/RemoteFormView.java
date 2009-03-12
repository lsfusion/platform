package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.form.RemoteFormInterface;
import platform.server.view.form.*;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.session.ChangeValue;

import java.sql.SQLException;
import java.io.*;
import java.util.List;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

// фасад для работы с клиентом
public class RemoteFormView extends UnicastRemoteObject implements RemoteFormInterface {

    RemoteForm form;
    public FormView richDesign;
    public JasperDesign reportDesign;

    public RemoteFormView(RemoteForm iForm, FormView iRichDesign, JasperDesign iReportDesign) throws RemoteException {
        super();
        
        form = iForm;
        richDesign = iRichDesign;
        reportDesign = iReportDesign;
    }

    public byte[] getReportDesignByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(outStream).writeObject(reportDesign);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getReportDataByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getFormData().serialize(new DataOutputStream(outStream));
        } catch (Exception e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    public byte[] getFormChangesByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.endApply().serialize(new DataOutputStream(outStream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public void changeGroupObject(int groupID, byte[] value) {
        
        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        try {
            form.changeGroupObject(groupObject, new GroupObjectValue(new DataInputStream(new ByteArrayInputStream(value)), groupObject));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getObjectClassID(int objectID) {
        return form.getObjectImplement(objectID).objectClass.ID;
    }

    public void changeGroupObject(int groupID, int changeType) {
        try {
            form.changeGroupObject(form.getGroupObjectImplement(groupID), changeType);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changePropertyView(int propertyID, byte[] object, boolean externalID) {
        try {
            form.changePropertyView(form.getPropertyView(propertyID), BaseUtils.deserializeObject(object), externalID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void changeObject(int objectID, Integer value) {
        try {
            form.changeObject(form.getObjectImplement(objectID), value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addObject(int objectID, int classID) {
        ObjectImplement object = form.getObjectImplement(objectID);
        try {
            form.addObject(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeClass(int objectID, int classID) {
        ObjectImplement object = form.getObjectImplement(objectID);
        try {
            form.changeClass(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeGridClass(int objectID,int idClass) {
        try {
            form.changeGridClass(form.getObjectImplement(objectID), idClass);
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
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

    public String saveChanges() {
        try {
            return form.saveChanges();
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
            form.getObjectImplement(objectID).baseClass.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) {

        try {
            ChangeValue changeValue = form.getPropertyEditorObjectValue(form.getPropertyView(propertyID), externalID);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            dataStream.writeBoolean(changeValue==null);
            if(changeValue!=null)
                changeValue.serialize(dataStream);

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
