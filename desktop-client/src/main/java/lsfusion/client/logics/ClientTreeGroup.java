package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.AbstractTreeGroup;
import lsfusion.interop.form.layout.FlexAlignment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTreeGroup extends ClientComponent implements ClientIdentitySerializable, AbstractTreeGroup<ClientContainer, ClientComponent> {

    public List<ClientGroupObject> groups = new ArrayList<>();

    public ClientToolbar toolbar;
    public ClientFilter filter;

    public boolean plainTreeMode;
    
    public boolean expandOnClick;

    public ClientTreeGroup() {

    }

    public ClientTreeGroup(int ID, ApplicationContext context) {
        super(ID, context);

        toolbar = new ClientToolbar(context);
        filter = new ClientFilter(context);
    }

    @Override
    protected void initDefaultConstraints() {
        flex = 1;
        alignment = FlexAlignment.STRETCH;
    }

    @Override
    public ClientComponent getToolbar() {
        return toolbar;
    }

    @Override
    public ClientComponent getFilter() {
        return filter;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, groups, serializationType);
        pool.serializeObject(outStream, toolbar, serializationType);
        pool.serializeObject(outStream, filter, serializationType);
        
        outStream.writeBoolean(expandOnClick);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        groups = pool.deserializeList(inStream);
        toolbar = pool.deserializeObject(inStream);
        filter = pool.deserializeObject(inStream);

        plainTreeMode = inStream.readBoolean();
        
        expandOnClick = inStream.readBoolean();

        List<ClientGroupObject> upGroups = new ArrayList<>();
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
