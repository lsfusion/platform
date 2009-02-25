package platform.client.interop;

import platform.interop.UserInfo;
import platform.interop.report.ReportData;
import platform.client.interop.classes.ClientClass;
import platform.client.navigator.ClientNavigatorElement;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import net.sf.jasperreports.engine.design.JasperDesign;

public class ByteDeSerializer {
    
    public static ClientFormChanges deserializeClientFormChanges(byte[] state, ClientFormView clientFormView) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return new ClientFormChanges(dataStream, clientFormView);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public static ClientClass deserializeClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return ClientClass.deserialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static ClientFormView deserializeClientFormView(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return new ClientFormView(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

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

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return ClientChangeValue.deserialize(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static ClientObjectValue deserializeClientObjectValue(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

        try {
            return new ClientObjectValue(dataStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) {

        ByteArrayInputStream inStream = new ByteArrayInputStream(state);
        DataInputStream dataStream = new DataInputStream(inStream);

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
