package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.client.ClientResourceBundle;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.AbstractTreeGroup;
import platform.interop.form.layout.SimplexConstraints;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTreeGroup extends ClientComponent implements ClientIdentitySerializable, AbstractTreeGroup<ClientContainer, ClientComponent> {

    public List<ClientGroupObject> groups = new ArrayList<ClientGroupObject>();

    public ClientToolbar toolbar;
    public ClientFilter filter;

    public boolean plainTreeMode;

    public ClientTreeGroup() {

    }

    public ClientTreeGroup(int ID, ApplicationContext context) {
        super(ID, context);

        toolbar = new ClientToolbar(context);
        filter = new ClientFilter(context);
    }

    @Override
    public ClientComponent getToolbar() {
        return toolbar;
    }

    @Override
    public ClientComponent getFilter() {
        return filter;
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getTreeDefaultConstraints(super.getDefaultConstraints());
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, groups, serializationType);
        pool.serializeObject(outStream, toolbar, serializationType);
        pool.serializeObject(outStream, filter, serializationType);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        plainTreeMode = inStream.readBoolean();

        List<ClientGroupObject> upGroups = new ArrayList<ClientGroupObject>();
        for (ClientGroupObject group : groups) {
            group.upTreeGroups.addAll(upGroups);
            upGroups.add(group);
        }
    }

    @Override
    public String getCaption() {
        return  ClientResourceBundle.getString("form.tree");
    }

    @Override
    public String toString() {
        String result = "";
        for (ClientGroupObject group : groups) {
            if (!result.isEmpty()) {
                result += ",";
            }
            result += group.toString();
        }
        return result + "[sid:" + getSID() + "]";
    }

    public int calculatePreferredSize() {
        int size = 0;
        for (ClientGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 35 * 4 : 35;
        }
        return size;
    }
}
