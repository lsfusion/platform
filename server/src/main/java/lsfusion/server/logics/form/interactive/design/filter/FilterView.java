package lsfusion.server.logics.form.interactive.design.filter;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FilterView extends ComponentView {

    public boolean visible = true;

    public FilterView() {
    }

    public FilterView(int ID) {
        super(ID);

        setAlignment(FlexAlignment.STRETCH);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();
    }
}
