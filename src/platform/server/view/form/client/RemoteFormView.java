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

// фасад для работы с клиентом
public class RemoteFormView implements RemoteFormInterface {

    RemoteForm form;
    public FormView richDesign;
    public JasperDesign reportDesign;

    public RemoteFormView(RemoteForm iForm, FormView iRichDesign, JasperDesign iReportDesign) {
        form = iForm;
        richDesign = iRichDesign;
        reportDesign = iReportDesign;
    }

    public byte[] getReportDesignByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(outStream).writeObject(reportDesign);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }

    public byte[] getReportDataByteArray() throws SQLException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getReportData().serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
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

    public byte[] getFormChangesByteArray() throws SQLException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.endApply().serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }

    public void changeGroupObject(int groupID, byte[] value) throws SQLException {
        
        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        try {
            form.changeGroupObject(groupObject, new GroupObjectValue(new DataInputStream(new ByteArrayInputStream(value)), groupObject));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getObjectClassID(int objectID) {
        return form.getObjectImplement(objectID).objectClass.ID;
    }

    public void changeGroupObject(int groupID, int changeType) throws SQLException {
        form.changeGroupObject(form.getGroupObjectImplement(groupID), changeType);
    }

    public void changePropertyView(int propertyID, byte[] object, boolean externalID) throws SQLException {
        form.changePropertyView(form.getPropertyView(propertyID), BaseUtils.deserializeObject(object), externalID);
    }

    public void changeObject(int objectID, Integer value) throws SQLException {
        form.changeObject(form.getObjectImplement(objectID), value);
    }

    public void addObject(int objectID, int classID) throws SQLException {
        ObjectImplement object = form.getObjectImplement(objectID);
        form.addObject(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
    }

    public void changeClass(int objectID, int classID) throws SQLException {
        ObjectImplement object = form.getObjectImplement(objectID);
        form.changeClass(object, (classID == -1) ? null : object.baseClass.findClassID(classID));
    }

    public void changeGridClass(int objectID,int idClass) throws SQLException {
        form.changeGridClass(form.getObjectImplement(objectID), idClass);
    }

    public void switchClassView(int groupID) throws SQLException {
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

    public String saveChanges() throws SQLException {
        return form.saveChanges();
    }

    public void cancelChanges() throws SQLException {
        form.cancelChanges();
    }

    public byte[] getBaseClassByteArray(int objectID) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            form.getObjectImplement(objectID).baseClass.serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            e.printStackTrace();
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

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) {
        
        ChangeValue changeValue = form.getPropertyEditorObjectValue(form.getPropertyView(propertyID), externalID);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);
        try {
            dataStream.writeBoolean(changeValue==null);
            if(changeValue!=null)
                changeValue.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outStream.toByteArray();
    }
}
