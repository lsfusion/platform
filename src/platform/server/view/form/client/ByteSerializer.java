package platform.server.view.form.client;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.interop.UserInfo;
import platform.interop.report.ReportData;
import platform.server.logics.ObjectValue;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.session.ChangeValue;
import platform.server.view.form.FormChanges;
import platform.server.view.navigator.NavigatorElement;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class ByteSerializer {
    // -------------------------------------- Сериализация классов -------------------------------------------- //
    public static byte[] serializeListClass(List<RemoteClass> classes) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeInt(classes.size());
            for (RemoteClass cls : classes)
                cls.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeClass(RemoteClass cls) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            cls.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeObjectValue(ObjectValue objectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            objectValue.objectClass.serialize(dataStream);
            BaseUtils.serializeObject(dataStream, objectValue.object);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeChangeValue(ChangeValue changeValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            dataStream.writeBoolean(changeValue ==null);
            if(changeValue !=null)
            changeValue.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeListNavigatorElement(List<NavigatorElement> listElements) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {

            dataStream.writeInt(listElements.size());

            for (NavigatorElement element : listElements)
                element.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeUserInfo(UserInfo userInfo) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(userInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();
    }

    public static byte[] serializeFormChanges(FormChanges formChanges) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            formChanges.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeReportData(ReportData reportData) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(reportData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeReportDesign(JasperDesign jasperDesign) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(jasperDesign);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeFormView(FormView formView) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            formView.serialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();
    }
}
