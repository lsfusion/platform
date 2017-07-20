package lsfusion.client.dock;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;
import lsfusion.base.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientDockableLayout implements MultipleCDockableLayout {
    private String canonicalName;

    public ClientDockableLayout() {
    }

    public ClientDockableLayout(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void readStream(DataInputStream in) throws IOException {
        canonicalName = SerializationUtil.readString(in);
    }

    public void writeStream(DataOutputStream out) throws IOException {
        SerializationUtil.writeString(out, canonicalName);
    }

    public void readXML(XElement element) {
        canonicalName = element.getString();
        if (canonicalName.isEmpty()) {
            canonicalName = null;
        }
    }

    public void writeXML(XElement element) {
        element.setString(canonicalName == null ? "" : canonicalName);
    }
}