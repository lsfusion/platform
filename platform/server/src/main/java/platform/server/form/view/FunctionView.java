package platform.server.form.view;

import platform.server.serialization.ServerSerializationPool;

import java.io.DataOutputStream;
import java.io.IOException;

public class FunctionView extends ComponentView implements ClientSerialize {

    String caption;

    public FunctionView(int ID, String caption) {
        super(ID);
        this.caption = caption;
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
    }
}
