package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorForm extends ClientNavigatorElement {
    
    public boolean isPrintForm;

    public ClientNavigatorForm() {

    }

    public ClientNavigatorForm(DataInputStream inStream) throws IOException {
        super(inStream);
        isPrintForm = inStream.readBoolean();
    }
}
