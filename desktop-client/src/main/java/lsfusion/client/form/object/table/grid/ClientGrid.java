package lsfusion.client.form.object.table.grid;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.object.ClientGroupObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientGridProperty {

    public final static int DEFAULT_HEADER_HEIGHT = 34;

    public boolean tabVertical = false;
    public boolean quickSearch;

    public ClientGroupObject groupObject;

    public ClientContainer record;

    public ClientGrid() {
    }

    @Override
    protected Integer getDefaultWidth() {
        return groupObject.getWidth(lineWidth);
    }

    @Override
    protected Integer getDefaultHeight() {
        return groupObject.getHeight(lineHeight, captionHeight);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        // Grid

        tabVertical = inStream.readBoolean();
        quickSearch = inStream.readBoolean();

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
        return captionHeight;
    }

    public void setHeaderHeight(int headerHeight) {
        this.captionHeight = headerHeight;
    }
}
