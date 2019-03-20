package lsfusion.client.form.filter.user;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPropertyFilterValue extends ClientFilterValue {

    public ClientPropertyDraw property;

    public String toString() { return ClientResourceBundle.getString("logics.property"); }

    byte getTypeID() {
        return 2;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(property.getID());
    }
}
