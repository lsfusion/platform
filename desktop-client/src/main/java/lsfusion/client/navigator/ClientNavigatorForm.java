package lsfusion.client.navigator;

import lsfusion.interop.ModalityType;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorForm extends ClientNavigatorElement {
    
    public ModalityType modalityType;

    public String formCanonicalName;
    public String formSID;
    
    public ClientNavigatorForm(DataInputStream inStream) throws IOException {
        super(inStream);
        modalityType = ModalityType.valueOf(inStream.readUTF());
        formCanonicalName = inStream.readUTF();
        formSID = inStream.readUTF();
    }
}
