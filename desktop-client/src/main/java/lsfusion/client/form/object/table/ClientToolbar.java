package lsfusion.client.form.object.table;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientToolbar extends ClientComponent {

    public boolean visible = true;

    public boolean showViews = true;
    public boolean showFilters = true;
    public boolean showSettings = true;
    public boolean showCountQuantity = true;
    public boolean showCalculateSum = true;
    public boolean showPrintGroupXls = true;
    public boolean showManualUpdate = true;

    public ClientToolbar() {
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();

        showViews = inStream.readBoolean();
        showFilters = inStream.readBoolean();
        showSettings = inStream.readBoolean();
        showCountQuantity = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showPrintGroupXls = inStream.readBoolean();
        showManualUpdate = inStream.readBoolean();
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.toolbar");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.toolbar") + "[sid:" + getSID() + "]";
    }
}
