package lsfusion.client.navigator;

import lsfusion.interop.ModalityType;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorForm extends ClientNavigatorElement {
    
    public ModalityType modalityType;

    public ClientNavigatorForm() {

    }

    public ClientNavigatorForm(int ID, String sID, String caption) {
        super(ID, sID, caption, false);
    }

    public ClientNavigatorForm(DataInputStream inStream) throws IOException {
        super(inStream);
        modalityType = ModalityType.valueOf(inStream.readUTF());
    }
}
