package platform.client.navigator;

import platform.interop.Constants;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorForm extends ClientNavigatorElement {
    
    public boolean isPrintForm;

    public ClientNavigatorForm() {

    }

    public ClientNavigatorForm(int ID, String caption) {
        super(ID, caption, false);

        sID = Constants.getDefaultFormSID(ID);
    }

    public ClientNavigatorForm(DataInputStream inStream) throws IOException {
        super(inStream);
        isPrintForm = inStream.readBoolean();
    }
}
