package platform.interop;

import platform.client.navigator.ClientNavigatorElement;
import platform.server.view.form.report.ReportData;
import platform.server.view.form.*;
import platform.server.view.navigator.NavigatorElement;
import platform.server.logics.classes.DataClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.UserInfo;
import platform.server.logics.session.ChangeValue;

import java.io.*;
import java.util.List;

import net.sf.jasperreports.engine.design.JasperDesign;

public class ByteArraySerializer extends Serializer {

    public static byte[] serializeClientFormView(ClientFormView clientFormView) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();
    }

    public static ClientFormView deserializeClientFormView(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (ClientFormView)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public static JasperDesign deserializeReportDesign(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (JasperDesign)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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

    public static ReportData deserializeReportData(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (ReportData)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // -------------------------------------- Сериализация данных формы -------------------------------------------- //

    public static byte[] serializeFormChanges(FormChanges formChanges) {

        //будем использовать стандартный OutputStream, чтобы кол-во передаваемых данных было бы как можно меньше
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeFormChanges(dataStream, formChanges);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientFormChanges deserializeClientFormChanges(byte[] state, ClientFormView clientFormView) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return Serializer.deserializeClientFormChanges(dataStream, clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static GroupObjectValue deserializeGroupObjectValue(byte[] state, GroupObjectImplement groupObject) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeGroupObjectValue(dataStream, groupObject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeClientGroupObjectValue(ClientGroupObjectValue clientGroupObjectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClientGroupObjectValue(dataStream, clientGroupObjectValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static byte[] serializeObject(Object value) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeObject(dataStream, value);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static Object deserializeObject(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeObject(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    // -------------------------------------- Сериализация классов -------------------------------------------- //
    public static byte[] serializeListClass(List<DataClass> classes) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeListClass(dataStream, classes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeListClientClass(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeClass(DataClass cls) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClass(dataStream, cls);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientClass deserializeClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientClass(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeObjectValue(ObjectValue objectValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeObjectValue(dataStream, objectValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientObjectValue deserializeClientObjectValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientObjectValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static byte[] serializeChangeValue(ChangeValue changeValue) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeChangeValue(dataStream, changeValue);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static ClientChangeValue deserializeClientChangeValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeClientChangeValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    // -------------------------------------- Сериализация фильтров -------------------------------------------- //
    public static byte[] serializeClientFilter(ClientFilter filter) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeClientFilter(dataStream, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static Filter deserializeFilter(byte[] state, RemoteForm remoteForm) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeFilter(dataStream, remoteForm);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    // -------------------------------------- Сериализация навигатора -------------------------------------------- //

    public static byte[] serializeListNavigatorElement(List<NavigatorElement> listElements) {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(outStream);

        try {
            serializeListNavigatorElement(dataStream, listElements);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outStream.toByteArray();

    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return deserializeListClientNavigatorElement(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

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

    public static UserInfo deserializeUserInfo(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);

        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return (UserInfo)dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
