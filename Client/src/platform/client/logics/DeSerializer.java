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

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state) throws IOException {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientNavigatorElement> listElements = new ArrayList<ClientNavigatorElement>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            listElements.add(ClientNavigatorElement.deserialize(dataStream));
        return listElements;
    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) throws IOException {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientClass> classes = new ArrayList<ClientClass>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            classes.add(ClientClass.deserialize(dataStream));
        return classes;
    }

}
