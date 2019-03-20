package lsfusion.client.form.filter;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientRegularFilterGroup extends ClientComponent {

    public List<ClientRegularFilter> filters = new ArrayList<>();

    public int defaultFilterIndex = -1;

    public ClientGroupObject groupObject;

    public ClientRegularFilterGroup() {

    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        pool.serializeCollection(outStream, filters);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        filters = pool.deserializeList(inStream);

        defaultFilterIndex = inStream.readInt();

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.filter");
    }

    @Override
    public String toString() {
        return filters.toString() + "[sid:" + getSID() + "]";
    }

}
