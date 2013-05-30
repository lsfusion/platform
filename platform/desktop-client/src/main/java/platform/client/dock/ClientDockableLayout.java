package platform.client.dock;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientDockableLayout implements MultipleCDockableLayout {
    private String formSID;

    public ClientDockableLayout() {
    }

    public ClientDockableLayout(String formSID) {
        this.formSID = formSID;
    }

    public String getFormSID() {
        return formSID;
    }

    public void readStream(DataInputStream in) throws IOException {
        formSID = in.readUTF();
    }

    public void writeStream(DataOutputStream out) throws IOException {
        out.writeUTF(formSID);
    }

    public void readXML(XElement element) {
        formSID = element.getString();
    }

    public void writeXML(XElement element) {
        element.setString(formSID);
    }
}