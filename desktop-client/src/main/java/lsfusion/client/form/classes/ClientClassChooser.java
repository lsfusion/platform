package lsfusion.client.form.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.object.ClientObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientObject object;

    public boolean visible = true;

    public ClientClassChooser() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeObject(outStream, object);
        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
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
        return ClientResourceBundle.getString("logics.classtree");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classtree") + " (" + object.toString() + ")" + "[sid:" + getSID() + "]";
    }
}
