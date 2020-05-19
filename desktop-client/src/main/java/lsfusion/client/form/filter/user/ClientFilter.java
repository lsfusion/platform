package lsfusion.client.form.filter.user;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFilter extends ClientComponent {
    public boolean visible = true;

    public ClientFilter() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateDependency(this, "visible");
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
