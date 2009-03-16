package platform.client;

import platform.interop.form.RemoteFormInterface;
import platform.interop.CompressingInputStream;
import platform.client.logics.ClientFormView;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import net.sf.jasperreports.engine.design.JasperDesign;

public class ClientObjectProxy {

    private static final Map<Integer, ClientFormView> cacheClientFormView = new HashMap();

    public static ClientFormView retrieveClientFormView(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        int ID = remoteForm.getID();

        if (!cacheClientFormView.containsKey(ID)) {

            byte[] state = remoteForm.getRichDesignByteArray();
            Log.incrementBytesReceived(state.length);

            cacheClientFormView.put(ID, new ClientFormView(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(state)))));
        }

        return cacheClientFormView.get(ID);
    }


    private static final Map<Integer, JasperDesign> cacheJasperDesign = new HashMap();

    public static JasperDesign retrieveJasperDesign(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        int ID = remoteForm.getID();

        if (!cacheJasperDesign.containsKey(ID)) {

            byte[] state = remoteForm.getReportDesignByteArray();
            Log.incrementBytesReceived(state.length);

            cacheJasperDesign.put(ID, (JasperDesign) new ObjectInputStream(new CompressingInputStream(new ByteArrayInputStream(state))).readObject());
        }

        return cacheJasperDesign.get(ID);
    }

}
