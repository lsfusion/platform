package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.CompressingOutputStream;
import platform.interop.RemoteObject;
import platform.interop.form.RemoteFormInterface;
import platform.server.data.classes.CustomClass;
import platform.server.view.form.*;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// фасад для работы с клиентом
public class RemoteFormView extends RemoteObject implements RemoteFormInterface {

    RemoteForm form;
    public FormView richDesign;
    public JasperDesign reportDesign;

    public RemoteFormView(RemoteForm iForm, FormView iRichDesign, JasperDesign iReportDesign, int port) throws RemoteException {
        super(port);
        
        form = iForm;
        richDesign = iRichDesign;
        reportDesign = iReportDesign;
    }

    public byte[] getReportDesignByteArray() {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            CompressingOutputStream compStream = new CompressingOutputStream(outStream);
            new ObjectOutputStream(compStream).writeObject(reportDesign);
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
            form.getFormData().serialize(new DataOutputStream(compStream));
            compStream.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public int getGID() {
        return form.GID;
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

        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        form.changePageSize(groupObject, pageSize);
    }

    public void gainedFocus() {
        form.gainedFocus();
    }

    public byte[] getFormChangesByteArray() {

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

    public void changeGroupObject(int groupID, byte[] value) {
        
        GroupObjectImplement groupObject = form.getGroupObjectImplement(groupID);
        try {
            DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(value));
            // считаем ключи и найдем groupObjectValue
            Map<ObjectImplement,Object> mapValues = new HashMap<ObjectImplement, Object>();
            for(ObjectImplement object : groupObject)
                mapValues.put(object, inStream.readInt());
            form.changeGroupObject(groupObject, groupObject.findGroupObjectValue(mapValues));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getObjectClassID(int objectID) {
        ObjectImplement objectImplement = form.getObjectImplement(objectID);
        if(!(objectImplement instanceof CustomObjectImplement))
            return 0;
        return (((CustomObjectImplement) objectImplement).currentClass).ID;
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
            form.getObjectImplement(objectID).changeValue(form.session, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addObject(int objectID, int classID) {
        CustomObjectImplement object = (CustomObjectImplement) form.getObjectImplement(objectID);
        try {
            form.addObject(object, (classID == -1) ? null : object.baseClass.findConcreteClassID(classID));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeClass(int objectID, int classID) {
        try {
            form.changeClass((CustomObjectImplement)form.getObjectImplement(objectID), classID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeGridClass(int objectID,int idClass) {
        ((CustomObjectImplement) form.getObjectImplement(objectID)).changeGridClass(idClass);
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
            form.addUserFilter(new CompareFilter(new DataInputStream(new ByteArrayInputStream(state)), form));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRegularFilter(int groupID, int filterID) {
        RegularFilterGroup filterGroup = form.getRegularFilterGroup(groupID);
        form.setRegularFilter(filterGroup, filterGroup.getFilter(filterID));
    }

    public int getID() {
        return form.ID;
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
            form.getObjectImplement(objectID).getBaseClass().serialize(new DataOutputStream(outStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outStream.toByteArray();
    }

    public byte[] getChildClassesByteArray(int objectID, int classID) {

        List<CustomClass> childClasses = (((CustomObjectImplement)form.getObjectImplement(objectID)).baseClass).findClassID(classID).children;

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

    public byte[] getPropertyChangeValueByteArray(int propertyID, boolean externalID) {

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            form.serializePropertyEditorObjectValue(dataStream,form.getPropertyView(propertyID), externalID);

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getPropertyValueClassByteArray(int propertyID) throws RemoteException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            form.getPropertyView(propertyID).view.property.getValueClass().serialize(dataStream);

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
