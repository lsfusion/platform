package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.form.RemoteFormInterface;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.RegularFilterGroup;
import platform.server.view.form.RemoteForm;

import java.sql.SQLException;

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
        return ByteSerializer.serializeReportDesign(reportDesign);
    }

    public byte[] getReportDataByteArray() throws SQLException {
        return ByteSerializer.serializeReportData(form.getReportData());
    }

    public int getGID() {
        return form.getGID();
    }

    public byte[] getRichDesignByteArray() {
        return ByteSerializer.serializeFormView(richDesign);
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    public byte[] getFormChangesByteArray() throws SQLException {
        return ByteSerializer.serializeFormChanges(form.endApply());
    }

    public void changeGroupObject(int groupID, byte[] value) throws SQLException {
        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        form.changeGroupObject(groupObject, ByteDeSerializer.deserializeGroupObjectValue(value, groupObject));
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
        form.addUserFilter(ByteDeSerializer.deserializeFilter(state, form));
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
        return ByteSerializer.serializeClass(form.getObjectImplement(objectID).baseClass);
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {
        return ByteSerializer.serializeListClass(form.getObjectImplement(objectID).baseClass.findClassID(classID).childs);
    }

    public byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) {
        return ByteSerializer.serializeChangeValue(form.getPropertyEditorObjectValue(form.getPropertyView(propertyID), externalID));
    }
}
