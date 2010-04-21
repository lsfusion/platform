package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.CompressingOutputStream;
import platform.interop.Order;
import platform.interop.RemoteObject;
import platform.interop.Scroll;
import platform.interop.form.RemoteFormInterface;
import platform.server.classes.CustomClass;
import platform.server.view.form.*;
import platform.server.view.form.filter.Filter;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// фасад для работы с клиентом
public class RemoteFormView extends RemoteObject implements RemoteFormInterface {

    RemoteForm<?> form;
    public FormView richDesign;
    public JasperDesign reportDesign;

    public RemoteFormView(RemoteForm form, FormView richDesign, JasperDesign reportDesign, int port) throws RemoteException {
        super(port);
        
        this.form = form;
        this.richDesign = richDesign;
        this.reportDesign = reportDesign;
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
            for(ObjectImplement object : groupObject.objects)
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

    public void changeGroupObject(int groupID, byte changeType) {
        try {
            form.changeGroupObject(form.getGroupObjectImplement(groupID), Scroll.deserialize(changeType));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changePropertyView(int propertyID, byte[] object) {
        try {
            form.changeProperty(form.getPropertyView(propertyID).view, BaseUtils.deserializeObject(object));
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

    public void changeClassView(int groupID, boolean show) {
        form.changeClassView(form.getGroupObjectImplement(groupID), show);
    }

    public void changePropertyOrder(int propertyID, byte modiType) {
        PropertyView propertyView = form.getPropertyView(propertyID);
        try {
            propertyView.toDraw.changeOrder(propertyView.view, Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeObjectOrder(int objectID, byte modiType) {
        ObjectImplement object = form.getObjectImplement(objectID);
        try {
            object.groupTo.changeOrder(object, Order.deserialize(modiType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearUserFilters() {

        for(GroupObjectImplement group : form.groups)
            group.clearUserFilters();
    }

    public void addFilter(byte[] state) {
        try {
            Filter filter = Filter.deserialize(new DataInputStream(new ByteArrayInputStream(state)), form);
            filter.getApplyObject().addUserFilter(filter);
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
        return form.navigatorForm.ID;
    }

    public void refreshData() {
        try {
            form.refreshData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public byte[] getPropertyChangeType(int propertyID) {

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            form.serializePropertyEditorType(dataStream,form.getPropertyView(propertyID));

            return outStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
