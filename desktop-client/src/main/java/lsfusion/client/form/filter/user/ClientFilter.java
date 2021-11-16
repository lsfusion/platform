package lsfusion.client.form.filter.user;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.base.view.FlexAlignment;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFilter extends ClientComponent {
    public ClientPropertyDraw property;
    public boolean fixed;

    public ClientFilter() {
    }
    
    public ClientFilter(ClientPropertyDraw property) {
        this.property = property;
        size = new Dimension(-1, -1);
        alignment = FlexAlignment.START;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);
        pool.serializeObject(outStream, property);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);
        property = pool.deserializeObject(inStream);
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.filter");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.filter") + "[sid:" + getSID() + "]";
    }
}
