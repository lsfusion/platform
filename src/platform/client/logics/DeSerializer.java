package platform.client.logics;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.client.logics.classes.ClientClass;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.form.ClientReportData;
import platform.interop.UserInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class DeSerializer {
    
    public static ClientFormChanges deserializeClientFormChanges(byte[] state, ClientFormView clientFormView) {

        try {
            return new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(state)), clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserInfo deserializeUserInfo(byte[] state) {

        try {
            return (UserInfo) new ObjectInputStream(new ByteArrayInputStream(state)).readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }        
        return null;
    }

    public static ClientReportData deserializeReportData(byte[] state) {

        try {
            return new ClientReportData(new DataInputStream(new ByteArrayInputStream(state)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JasperDesign deserializeReportDesign(byte[] state) {

        try {
            return (JasperDesign) new ObjectInputStream(new ByteArrayInputStream(state)).readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClientClass deserializeClientClass(byte[] state) {

        try {
            return ClientClass.deserialize(new DataInputStream(new ByteArrayInputStream(state)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClientFormView deserializeClientFormView(byte[] state) {

        try {
            return new ClientFormView(new DataInputStream(new ByteArrayInputStream(state)));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        try {
            List<ClientNavigatorElement> listElements = new ArrayList<ClientNavigatorElement>();
            int count = dataStream.readInt();
            for (int i = 0; i < count; i++)
                listElements.add(ClientNavigatorElement.deserialize(dataStream));
            return listElements;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClientChangeValue deserializeClientChangeValue(byte[] state) {

        try {
            return ClientChangeValue.deserialize(new DataInputStream(new ByteArrayInputStream(state)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ClientObjectValue deserializeClientObjectValue(byte[] state) {

        try {
            return new ClientObjectValue(new DataInputStream(new ByteArrayInputStream(state)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        try {
            List<ClientClass> classes = new ArrayList<ClientClass>();
            int count = dataStream.readInt();
            for (int i = 0; i < count; i++)
                classes.add(ClientClass.deserialize(dataStream));
            return classes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
