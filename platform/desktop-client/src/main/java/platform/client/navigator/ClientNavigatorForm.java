package platform.client.navigator;

import platform.interop.ModalityType;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorForm extends ClientNavigatorElement {
    
    public boolean isPrintForm;
    public ModalityType modalityType;

    public ClientNavigatorForm() {

    }

    public ClientNavigatorForm(int ID, String sID, String caption) {
        super(ID, sID, caption, false);
    }

    public ClientNavigatorForm(DataInputStream inStream) throws IOException {
        super(inStream);
        isPrintForm = inStream.readBoolean();
        modalityType = ModalityType.valueOf(inStream.readUTF());
    }
}
