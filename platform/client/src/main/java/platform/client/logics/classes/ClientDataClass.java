package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public abstract class ClientDataClass extends ClientClass implements ClientType {

    public ClientDataClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public boolean hasChilds() {
        return false;
    }

    public ClientType getType() {
        return this;
    }

    public int getMinimumWidth() {
        return getPreferredWidth();
    }
    public int getPreferredWidth() {
        return 50;
    }
    public int getMaximumWidth() {
        return Integer.MAX_VALUE;
    }
}
