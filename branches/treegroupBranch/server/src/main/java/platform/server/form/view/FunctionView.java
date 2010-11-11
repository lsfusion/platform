package platform.server.form.view;

import platform.interop.form.layout.AbstractFunction;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FunctionView extends ComponentView implements AbstractFunction<ContainerView, ComponentView> {

    String caption;

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public FunctionView() {

    }
    
    public FunctionView(int ID) {
        super(ID);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        setCaption(pool.readString(inStream));
    }
}
