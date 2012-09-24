package platform.server.form.view;

import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ToolbarView extends ComponentView {
    public boolean visible = true;

    public boolean showGroupChange = true;
    public boolean showCountQuantity = true;
    public boolean showCalculateSum = true;
    public boolean showGroup = true;
    public boolean showPrintGroupButton = true;
    public boolean showPrintGroupXlsButton = true;
    public boolean showHideSettings = true;

    public ToolbarView() {

    }

    public ToolbarView(int ID) {
        super(ID);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(visible);

        outStream.writeBoolean(showGroupChange);
        outStream.writeBoolean(showCountQuantity);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showGroup);
        outStream.writeBoolean(showPrintGroupButton);
        outStream.writeBoolean(showPrintGroupXlsButton);
        outStream.writeBoolean(showHideSettings);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();

        showGroupChange = inStream.readBoolean();
        showCountQuantity = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showGroup = inStream.readBoolean();
        showPrintGroupButton = inStream.readBoolean();
        showPrintGroupXlsButton = inStream.readBoolean();
        showHideSettings = inStream.readBoolean();
    }
}
