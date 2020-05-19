package lsfusion.client.form.object.table.grid;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.object.ClientGroupObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientComponent {

    public boolean tabVertical = false;
    public boolean quickSearch;
    public int headerHeight;

    public ClientGroupObject groupObject;

    public ClientComponent record;

    public ClientGrid() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(quickSearch);
        outStream.writeInt(headerHeight);

        pool.serializeObject(outStream, record);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        tabVertical = inStream.readBoolean();
        quickSearch = inStream.readBoolean();
        headerHeight = inStream.readInt();

        record = pool.deserializeObject(inStream);

        groupObject = pool.deserializeObject(inStream);
    }

    public String getCaption() {
        return ClientResourceBundle.getString("logics.grid");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.grid") + " (" + groupObject.toString() + ")" + "[sid:" + getSID() + "]";
    }

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
        updateDependency(this, "tabVertical");
    }

    public boolean getTabVertical() {
        return tabVertical;
    }

    public int getHeaderHeight() {
        return headerHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }
}
